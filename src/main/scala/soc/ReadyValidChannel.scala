package soc

import chisel3.util.DecoupledIO
import chisel3._

class ReadyValidChannel[T <: Data](private val dt: T) extends Bundle {
  val tx = Flipped(new DecoupledIO(dt))
  val rx = new DecoupledIO(dt)
}
