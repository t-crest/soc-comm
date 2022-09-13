package soc

import chisel3.util.DecoupledIO
import chisel3._

/**
  * A dual direction channel. Direction seen from the driver (tx is an output port, rx an input port).
  * @param dt
  * @tparam T
  */
class ReadyValidChannelsIO[T <: Data](private val dt: T) extends Bundle {
  val tx = new DecoupledIO(dt)
  val rx = Flipped(new DecoupledIO(dt))
}
