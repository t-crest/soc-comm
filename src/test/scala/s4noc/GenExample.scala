

package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

class GenExample extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "GenExample"

  class Elem[T <: Data](private val dt: T) extends Bundle {
    val data = dt.cloneType
    val time = UInt(8.W)
  }

  class M[T <: Data](dt: T) extends Module {
    val io = IO(new Bundle {
      val in = Input(new Elem(dt))
      val out = Output(new Elem(dt))
    })

    io.out.data := io.in.data
    io.out.time := 2.U
  }

  it should "work" in {
    test(new M(UInt(16.W))) { c =>

      // Following does not run
      val e = Wire(new Elem(UInt(16.W)))
      e.data := 1.asUInt(16.W)
      e.time := 2.asUInt
      c.io.in.poke(e)

      // .Lit does not work in the following, even not on 0.2-SNAPSHOT
      // c.io.in.poke(chiselTypeOf(c.io.in).Lit(_.data -> 1.U, _.time -> 2.U))


      c.clock.step(1)
      c.io.out.data.expect(1.U)
    }
  }
}