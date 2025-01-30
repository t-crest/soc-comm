/*
 * A simple IO interface.
 * rdy is used for acknowledgement in the following cycle (like OCP core in Patmos).
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 *
 */

package soc

import chisel3._
import chisel3.util.DecoupledIO

abstract class MultiCoreDevice(nrCores: Int, addrWidth: Int) extends Module {
  val ports = IO(Vec(nrCores, new PipeCon(addrWidth)))
}

/**
  * A dual direction channel. Direction seen from the driver (tx is an output port, rx an input port).
  * @param dt
  * @tparam T
  */
class ReadyValidChannelsIO[T <: Data](private val dt: T) extends Bundle {
  val tx = new DecoupledIO(dt)
  val rx = Flipped(new DecoupledIO(dt))
}
