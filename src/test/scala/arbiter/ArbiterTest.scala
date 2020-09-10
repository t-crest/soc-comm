package arbiter

import chisel3._
import chiseltest._
import org.scalatest._


class ArbiterTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Arbiter"

  "DualArbiter" should "fail" in {
    test(new DualArbiter(UInt(8.W))) { d =>
      d.io.in(0).valid.poke(true.B)
      d.io.in(1).valid.poke(true.B)
      d.io.in(0).bits.poke(123.U)
      d.io.in(1).bits.poke(234.U)
      d.clock.step(1)
      // d.io.out.bits.expect(345.U)

    }
  }
}


