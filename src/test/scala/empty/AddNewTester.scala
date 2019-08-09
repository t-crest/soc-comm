package empty

import org.scalatest._

import chisel3._
import chisel3.tester._

class AddNewTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Adder with Testers2"

  it should "test addition" in {
    test(new Add()) { c =>
      for (a <- 0 to 2) {
        for (b <- 0 to 3) {
          val result = a + b
          c.io.a.poke(a.U)
          c.io.b.poke(b.U)
          c.clock.step(1)
          c.io.c.expect(result.U)
        }
      }

    }

  }

}