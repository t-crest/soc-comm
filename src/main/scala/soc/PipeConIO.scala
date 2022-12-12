package soc

import chisel3._

/**
  * A pipelined interface called PipeCon, similar to the OCPCore in the Patmos project.
  * Signal direction from the CPU.
  * @param addrWidth
  */
class PipeConIO(private val addrWidth: Int) extends Bundle {
  val address = Output(UInt(addrWidth.W))
  val rd = Output(Bool())
  val wr = Output(Bool())
  val rdData = Input(UInt(32.W))
  val wrData = Output(UInt(32.W))
  val wrMask = Output(UInt(4.W))
  val ack = Input(Bool())
}
