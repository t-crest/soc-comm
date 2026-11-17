package slimflit.buffers

import chisel3._
import chisel3.util._

import slimflit._


class SkidBuffer[T <: Data](d: T) extends Module {

  val io = IO(new QueueIO(d, 2, hasFlush = false))
  
  val mainValid = RegInit(0.B)
  val mainData  = Reg(d.cloneType)
  val skidValid = RegInit(0.B)
  val skidData  = Reg(d.cloneType)

  io.enq.ready  := !skidValid
  io.deq.valid := mainValid
  io.deq.bits  := mainData

  // main register can take a new beat this cycle
  val mainFree = !mainValid || io.deq.ready

  when(mainFree) {
    when(skidValid) {
      // drain skid into main; input is blocked (in.ready = 0)
      mainValid := 1.B
      mainData  := skidData
      skidValid := 0.B
    }.otherwise {
      // pass-through: input into main
      mainValid := io.enq.valid
      when(io.enq.valid) { mainData := io.enq.bits }
    }
  }.otherwise {
    // main stalled: beat accepted under stale ready lands in skid
    when(io.enq.fire) {
      skidValid := 1.B
      skidData  := io.enq.bits
    }
  }

  io.count := mainValid + skidValid

}

object SkidBuffer extends BufferFactory {
  def name: String = "SkidBuffer"
  def buffer[T <: Data](d: T, depth: Int) = {
    require(depth == 2, "SkidBuffer only supports depth of 2")
    Module(new SkidBuffer(d)).io
  }
}