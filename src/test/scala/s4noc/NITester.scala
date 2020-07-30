package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._
import chisel3.experimental.BundleLiterals._

class NITester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "NI"

  it should "work" in {
    test(new NetworkInterface(4, 2, 2, UInt(16.W))) { dut =>

      dut.io.networkPort.tx.bits.poke(chiselTypeOf(dut.io.networkPort.tx.bits).Lit(_.data -> 1.U, _.time -> 2.U))
      dut.io.networkPort.tx.valid.poke(true.B)

      for (i <- 1 until 30) {
        println(dut.io.networkPort.tx.ready.peek.litToBoolean)
        println(dut.io.local.out.data.peek.litValue)
        dut.clock.step()
        dut.io.networkPort.tx.valid.poke(false.B)
      }
      dut.clock.step(10)
      // may take more than one clock cycle...
      dut.io.local.out.data.expect(1.U)
    }
  }
}