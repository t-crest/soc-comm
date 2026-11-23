package slimflit.buffers

import chisel3._
import chisel3.util._

import slimflit._

/**
  * A buffer factory creates a buffer for a given DecoupledIO interface and depth.
  */
trait BufferFactory {
  def name: String
  def create[T <: Data](d: SlimPackPort[T], depth: Int)(implicit p: SlimFlitParams[T]): InputBuffer[T] = {
    new InputBuffer(buffer(new SlimPack[T], depth), d.dir)
  }

  def buffer[T <: Data](d: T, depth: Int): Module { val io: QueueIO[T] }
}

class InputBuffer[T <: Data](
  b: => Module { val io: QueueIO[SlimPack[T]] },
  val dir: Direction
)(implicit p: SlimFlitParams[T]) extends Module {
  val enq = IO(Flipped(new SlimPackPort[T](dir)))
  val deq = IO(new SlimPackPort[T](dir))

  val buf = Module(b)
  buf.io.enq <> enq
  deq <> buf.io.deq
}