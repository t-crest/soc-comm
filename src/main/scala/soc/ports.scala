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

/**
  * A simple IO interface, as seen from the slave.
  * ack is used for acknowledgement in the following clock cycle, or later. (like OCPcore in Patmos).
  * Can be used to stall the CPU.
  *
  * @param addrWidth width of the address part
  */
class PipeConIO(private val addrWidth: Int) extends Bundle {
  val address = Input(UInt(addrWidth.W))
  val rd = Input(Bool())
  val wr = Input(Bool())
  val rdData = Output(UInt(32.W))
  val wrData = Input(UInt(32.W))
  val wrMask = Input(UInt(4.W))
  val ack = Output(Bool())
}

abstract class MultiCoreDevice(nrCores: Int, addrWidth: Int) extends Module {
  val ports = IO(Vec(nrCores, new PipeConIO(addrWidth)))
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
