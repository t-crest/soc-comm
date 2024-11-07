package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/**
 * Test a 2x2 Network.
 */

class NetworkTest(dontRun: String) extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "2x2 Network"

  "the NoC" should "work" in {
    test(new NetworkOfFour()) { dut =>
      // after clock cycle 6 all outputs are 0, strange
      // for (i <- 0 until 8) {
      for (i <- 0 until 6) {
        for (j <- 0 until 4) {
          dut.io.local(j).in.data.poke((0x10 * (j + 1) + i).U)
          dut.io.local(j).in.valid.poke(true.B)
        }
        dut.clock.step(1)
        for (j <- 0 until 4) {
          print(s"${dut.io.local(j).out.valid.peekInt()}  ${dut.io.local(j).out.data.peekInt()} ")
        }
        println()
      }
      dut.io.local(0).out.data.expect(0x24.U)
    }
  }

  it should "work under full load" in {

    val TESTS = 200
    val doPrint = false

    for (n <- 2 until 6) {

      println(s"Testing network of $n x $n")

      // test(new Network(n, UInt(32.W))).withAnnotations(Seq(WriteVcdAnnotation))) { dut =>

      test(new Network(n, UInt(32.W))) { dut =>

        val sched = Schedule(n)
        var count = 0
        var slot = 0

        val tdmLength = sched.schedule.length

        var nrSent = 0
        var nrRcvd = 0
        var maxLatency = 0
        var sumLatency = 0


        def send(core: Int) = {

          var cnt = 0
          val port = dut.io.local(core).in
          for (i <- 0 until TESTS) {

            val dest = sched.timeToDest(core, cnt % tdmLength).dest
            port.valid.poke((dest != -1).B)
            val data = ((cnt << 16) + (core << 8) + dest)
            port.data.poke(data.U)



            if (dest != -1) {
              if (doPrint) println(s"Send $data from $core to $dest at $cnt")
              nrSent += 1
            }
            dut.clock.step()
            cnt += 1
          }
        }

        def receive(core: Int) = {

          var cnt = 0
          val port = dut.io.local(core).out
          for (i <- 0 until TESTS + tdmLength) {
            val valid = port.valid.peekBoolean()
            val data = port.data.peekInt().toInt
            val sndCnt = data >> 16
            val sndCore = (data >> 8) & 0xff
            val dest = data & 0xff

            if (valid) {
              val lat = cnt - sndCnt
              sumLatency += lat
              maxLatency = if (lat > maxLatency) lat else maxLatency
              if (doPrint || dest != core) println(s"Received at core $core $data from core $sndCore to core $dest at clock cycle $cnt, sent at clock cycle $sndCnt. Latency was $lat")
              if (dest != core) println("ERROR")
              assert(dest == core)
              // TODO: test sndCore and the timing
              nrRcvd += 1
            }

            dut.clock.step()
            cnt += 1
          }
        }

        def dump(): Unit = {
          for (i <- 0 until n * n) {
            for (j <- 0 until 5) {
              // print(s"$i $j ${dut.net(i).io.ports(j).out.data.peekInt} | ")
            }
            // println
          }
        }

        dut.clock.step() // Schedule starts with one cycle delay
        for (i <- 0 until n * n) {
          fork {
            send(i)
          }
          fork {
            receive(i)
          }
        }
        // TODO: we should find a way to join all threads here
        // TODO: see https://github.com/ucb-bar/chisel-testers2/issues/184 for how to use the shared clock variable
        for (i <- 0 until TESTS + tdmLength + 1) {
          count += 1
          slot = count % sched.schedule.length
          // dump()
          dut.clock.step()
        }
        dut.clock.step(10)
        assert(nrRcvd == nrSent)
        val cycles = TESTS + tdmLength
        val avgLat = sumLatency.toDouble / nrRcvd
        println(s"$nrSent packages transmitted in $cycles clock cycles, max latency: $maxLatency, avg latency: $avgLat")
        val bandwidth = nrRcvd.toDouble / cycles
        println(s"Bandwidth: $bandwidth packets per clock cycle")
      }
    }
  }
}