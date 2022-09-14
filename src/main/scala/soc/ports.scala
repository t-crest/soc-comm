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
  * rdy is used for acknowledgement in the following clock cycle, or later. (like OCPcore in Patmos).
  * Can be used to stall the CPU.
  *
  * @param addrWidth width of the address part
  */
class CpuPortIO(private val addrWidth: Int) extends Bundle {
  val wr = Input(Bool())
  val rd = Input(Bool())
  val address = Input(UInt(addrWidth.W))
  val wrData = Input(UInt(32.W))
  val rdData = Output(UInt(32.W))
  val ack = Output(Bool())
}

class MultiPortIO(private val nrPorts: Int, private val addrWidth: Int) extends Bundle {
  val ports = Vec(nrPorts, new CpuPortIO(addrWidth))
}

abstract class MultiCoreDevice(nrCores: Int, addrWidth: Int) extends Module {
  val io = IO(new MultiPortIO(nrCores, addrWidth))
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
