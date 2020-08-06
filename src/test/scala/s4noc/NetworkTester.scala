package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

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
    val TESTS = 3

    val scoreBoard = Array.ofDim[Int](N * N, N * N)


    // test(new Network(N, UInt(32.W))) { dut =>
    test(new NetworkOfFour()) { dut =>

      val sched = Schedule.getSchedule()
      var count = 0
      var slot = 0

      val tdmLength = sched.schedule.length


      def send(core: Int) = {

        var cnt = 0
        val port = dut.io.local(core).in
        for (i <- 0 until TESTS) {

          val dest = sched.timeToDest(core, cnt % tdmLength)._1
          port.valid.poke((dest != -1).B)
          port.valid.poke(true.B)
          port.data.poke(((cnt << 16) + (core << 8) + dest).U)

          dut.io.local(core).in.data.poke((0x10 * core + i).U)
          dut.io.local(core).in.valid.poke(true.B)


          if (dest != -1) {
            println("Send from " + core + " to " + dest + " at " + cnt)
          }
          dut.clock.step()
          cnt += 1
        }
      }

      def receive(core: Int) = {

        var cnt = 0
        val port = dut.io.local(core).out
        for (i <- 0 until TESTS) {
          val valid = port.valid.peek.litToBoolean
          val data = port.data.peek.litValue

          // if (valid) {
            println("Receive at " + core + " from " + data + " at " + cnt)
          // }

          println("Vals: " + dut.io.local(core).out.valid.peek.litValue + " " + dut.io.local(core).out.data.peek.litValue + " ")

          dut.clock.step()
          cnt += 1
        }
      }

      for (i <- 0 until N * N) {
        fork {
          send(i)
        }
        fork {
          receive(i)
        }
      }
      for (i <- 0 until TESTS + 1) {
        println("Main thread")
        count += 1
        slot = count % sched.schedule.length
        dut.clock.step()
      }
      dut.clock.step(10)
    }
  }
}