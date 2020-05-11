package arbiter

import chisel3._
import chisel3.util._

/**
  * An arbiter between two ready/valid requests.
  *
  * Can be extended by functional generation of a tree.
  *
  * @param gen
  * @tparam T
  *
  * val ready = Input(Bool())
  * val valid = Output(Bool())
  * val bits  = Output(genType)
  */
class DualArbiter[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle() {
    val in = Flipped(Vec(2, new DecoupledIO(gen)))
    val out = new DecoupledIO(gen)
  })

  // is <> ok? Or broken with Chisel 3?
  io.out <> io.in(0)

  // io.in(0).ready := io.out.ready
  io.in(1).ready := false.B
}

object DualArbiter extends App {
  chisel3.Driver.execute(Array[String](), () => new DualArbiter(UInt(8.W)))
}

