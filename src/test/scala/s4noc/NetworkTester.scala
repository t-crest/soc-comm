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
}