/*
 * A simple IO interface.
 * rdy is used for acknowledgement in the following cycle (like OCP core in Patmos).
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 *
 */

package soc

import chisel3._

/**
  * A simple IO interface, as seen from the slave.
  * rdy is used for acknowledgement in the following cycle (like OCPio in Patmos).
  *
  * @param addrWidth width of the address part
  */
class IOPort(private val addrWidth: Int) extends Bundle {
  val wr = Input(Bool())
  val rd = Input(Bool())
  val address = Input(UInt(addrWidth.W))
  val wrData = Input(UInt(32.W))
  val rdData = Output(UInt(32.W))
  val rdy = Output(Bool())
}

class MultiPort(private val nrPorts: Int, private val addrWidth: Int) extends Bundle {
  val ports = Vec(nrPorts, new IOPort(addrWidth))
}

abstract class MultiCoreDevice(nrCores: Int, addrWidth: Int) extends Module {
  val io = IO(new MultiPort(nrCores, addrWidth))
}
