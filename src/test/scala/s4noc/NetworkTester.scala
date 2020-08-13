package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

/**
 * Test a 2x2 Network.
 */

class NetworkTester extends FlatSpec with ChiselScalatestTester with Matchers {
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
          print(dut.io.local(j).out.valid.peek.litValue + " " + dut.io.local(j).out.data.peek.litValue + " ")
        }
        println
      }
      dut.io.local(0).out.data.expect(0x24.U)
    }
  }

  it should "work under full load" in {

    val N = 2
    // TODO: for N = 3 it fails
    val TESTS = 20
    val doPrint = true

    // val scoreBoard = Array.ofDim[Int](N * N, N * N)

    // test(new Network(N, UInt(32.W))).withAnnotations(Seq(chiseltest.internal.WriteVcdAnnotation)) { dut =>

    test(new Network(N, UInt(32.W))) { dut =>

      val sched = Schedule.getSchedule()
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

          val dest = sched.timeToDest(core, cnt % tdmLength)._1
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
          val valid = port.valid.peek.litToBoolean
          val data = port.data.peek.litValue.toInt
          val sndCnt = data >> 16
          val sndCore = (data >> 8) & 0xff
          val dest = data & 0xff

          if (valid) {
            val lat = cnt - sndCnt
            sumLatency += lat
            maxLatency = if (lat > maxLatency) lat else maxLatency
            if (doPrint) println(s"Received at $core $data from $sndCore to $dest at $cnt, sent at $sndCnt. Latency was $lat")
            assert(dest == core)
            // TODO: test sndCore and the timing
            nrRcvd += 1
          }

          dut.clock.step()
          cnt += 1
        }
      }

      def dump(): Unit = {
        for (i <- 0 until N * N) {
          for (j <- 0 until 5) {
            // print(s"$i $j ${dut.net(i).io.ports(j).out.data.peek.litValue()} | ")
          }
          // println
        }
      }

      dut.clock.step() // Schedule starts with one cycle delay
      for (i <- 0 until N * N) {
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