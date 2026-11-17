package slimflit.buffers

import chisel3._
import chisel3.util._

import slimflit._

object BubbleBuffer extends BufferFactory {
  def name: String = "BubbleBuffer"
  def buffer[T <: Data](d: T, depth: Int) = Module(new BubbleBuffer(d, depth)).io
}

class BubbleBuffer[T <: Data](d: T, depth: Int) extends Module {
  val io = IO(new QueueIO(d, depth, hasFlush = false))

  Seq.fill(depth)(Module(new BubbleStage(d)))
    .foldLeft(io.enq) { (prev, stage) =>
      stage.io.enq <> prev
      stage.io.deq
    } <> io.deq

}

class BubbleStage[T <: Data](d: T) extends Module {
  val io = IO(new QueueIO(d, 1, hasFlush = false))

  val valid = RegInit(0.B)
  val data  = Reg(d.cloneType)

  io.enq.ready := !valid
  io.deq.valid := valid
  io.deq.bits  := data
}