package slimflit.buffers

import chisel3._
import chisel3.util._

import slimflit._

object BubbleBuffer extends BufferFactory {
  def name: String = "BubbleBuffer"
  def buffer[T <: Data](d: T, depth: Int) = new BubbleBuffer(d, depth)
}

class BubbleBuffer[T <: Data](d: T, depth: Int) extends Module {
  val io = IO(new QueueIO(d, depth, hasFlush = false))

  val (out, valids) = Seq.fill(depth)(Module(new BubbleStage(d)))
    .foldLeft(io.enq -> Seq.empty[Bool]) { case ((prev, valids), stage) =>
      stage.io.enq <> prev
      stage.io.deq -> (valids :+ stage.io.deq.valid)
    } 
    
  out <> io.deq

  io.count := PopCount(valids)
  

}

class BubbleStage[T <: Data](d: T) extends Module {
  val io = IO(new QueueIO(d, 1, hasFlush = false))

  val valid = RegInit(0.B)
  val data  = Reg(d.cloneType)

  io.enq.ready := !valid
  io.deq.valid := valid
  io.deq.bits  := data
  io.count := valid

  when(io.enq.fire) {
    valid := 1.B
    data  := io.enq.bits
  }.elsewhen(io.deq.fire) {
    valid := 0.B
  }
}