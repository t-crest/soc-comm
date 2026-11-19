package slimflit.arbiter

import chisel3._
import chisel3.util._

trait ArbiterFactory { 
  def name: String
  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] }
  def firstComeFirstServe: Boolean
}

class Arbiter[T <: Data](d: T, n: Int, mutex: => Module { val io: MutexIO }) extends Module {
  val io = IO(new ArbiterIO(d, n))

  val m = Module(mutex)

  m.io.req := VecInit(io.in.map(_.valid))

  io.chosen := OHToUInt(m.io.gnt)

  m.io.rel := VecInit.fill(n)(io.out.ready)

  val masked = io.in.zip(m.io.gnt).map { case (in, gnt) => in.bits.asUInt & Fill(d.getWidth, gnt) }

  io.out.bits := VecInit(masked).reduceTree(_ | _).asTypeOf(d)
  io.out.valid := VecInit(io.in.map(_.valid)).reduceTree(_ || _)

  io.in.zip(m.io.gnt).foreach { case (in, gnt) =>
    in.ready := gnt && io.out.ready
  }

}

object ChiselRRArbiter extends ArbiterFactory {
  def name: String = "ChiselRRArbiter"
  def firstComeFirstServe: Boolean = false
  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] } = new chisel3.util.RRArbiter(d, n)
}



class MutexIO(n: Int) extends Bundle {
  val req = Input(Vec(n, Bool()))
  val gnt = Output(Vec(n, Bool()))
  val rel = Input(Vec(n, Bool()))
}



