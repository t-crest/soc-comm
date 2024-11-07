package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
// import chisel3.experimental.BundleLiterals._

class NITester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "NI"

  it should "work" in {
    test(new NetworkInterface(0, Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 16), UInt(16.W))) { dut =>

      // dut.io.networkPort.tx.bits.poke(chiselTypeOf(dut.io.networkPort.tx.bits).Lit(_.data -> 1.U, _.time -> 2.U))
      dut.io.networkPort.tx.bits.data.poke(1.U)
      dut.io.networkPort.tx.bits.core.poke(2.U)
      dut.io.networkPort.tx.valid.poke(true.B)

      var pass = false

      for (i <- 1 until 30) {
        dut.clock.step()
        dut.io.networkPort.tx.valid.poke(false.B)
        if (dut.io.local.in.data.peekInt() == 1) pass = true
       }
      assert(pass)
    }
  }
}