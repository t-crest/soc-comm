package slimflit.arbiter

import chisel3._
import chisel3.util._

trait ArbiterFactory { 
  def create[T <: Data](d: DecoupledIO[T], depth: Int): Arbiter[T]
}

abstract class Arbiter[T <: Data]() extends Module {
  val io: ArbiterIO[T]
}


class MutexIO(n: Int) extends Bundle {
  val req = Input(Vec(n, Bool()))
  val gnt = Output(Vec(n, Bool()))
  val rel = Input(Vec(n, Bool()))
}



