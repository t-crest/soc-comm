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
    val buf = buffer(d.bits, depth)
    InputBuffer(d.dir, buf.enq.asTypeOf(d), buf.deq.asTypeOf(d))
  }

  def buffer[T <: Data](d: T, depth: Int): QueueIO[T]
}

case class InputBuffer[T <: Data](
  dir: Direction,
  enq: SlimPackPort[T],
  deq: SlimPackPort[T]
)