

package s4noc

import chisel3._
import chiseltest._
import org.scalatest._
import chisel3.experimental.BundleLiterals._

class GenExample extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "GenExample"

  class Elem[T <: Data](private val dt: T) extends Bundle {
    val data = dt.cloneType
    val time = UInt(8.W)

    def myPoke(d: T, t: UInt): Unit = {
      data.poke(d)
      time.poke(t)
    }
  }

  class M[T <: Data](dt: T) extends Module {
    val io = IO(new Bundle {
      val in = Input(new Elem(dt))
      val out = Output(new Elem(dt))
    })

    io.out.data := io.in.data
    io.out.time := io.in.time + 1.U

  }

  it should "work with an experimental bundle literal" in {
    test(new M(UInt(16.W))) { c =>

      c.io.in.poke(chiselTypeOf(c.io.in).Lit(_.data -> 1.U, _.time -> 2.U))
      c.clock.step(1)
      c.io.out.data.expect(1.U)
    }
  }

  it should "work with helper function" in {
    test(new M(UInt(16.W))) { c =>

      c.io.in.myPoke(3.U, 4.U)
      c.clock.step(1)
      c.io.out.data.expect(3.U)
    }
  }

  it should "work directly" in {
    test(new M(UInt(16.W))) { c =>

      c.io.in.data.poke(13.U)
      c.io.in.time.poke(4.U)
      c.clock.step(1)
      c.io.out.data.expect(13.U)
    }
  }
}