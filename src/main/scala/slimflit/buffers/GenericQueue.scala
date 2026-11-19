package slimflit.buffers

import chisel3._
import chisel3.util._

import slimflit._

/**
  * A generic queue buffer of a given depth.
  */
object GenericQueue extends BufferFactory {
  def name: String = "GenericQueue"
  def buffer[T <: Data](d: T, depth: Int) = 
    new Queue(
      gen = d, 
      entries = depth,
      pipe = false,
      flow = false,
      useSyncReadMem = true,
      hasFlush = false
    )
}