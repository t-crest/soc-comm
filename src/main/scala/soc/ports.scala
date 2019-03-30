/*
 * A simple IO interface.
 * rdy is used for acknowledgement in the following cycle (like OCP core in Patmos).
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 *
 */

package soc

import Chisel._

class Port(addrWidth: Int) extends Bundle {
  val wr = Input(Bool())
  val rd = Input(Bool())
  val address = Input(UInt(width = addrWidth))
  val wrData = Input(UInt(width = 32))
  val rdData = Output(UInt(width = 32))
  val rdy = Output(Bool())
}

class MultiPort(nrPorts: Int, addrWidth: Int) extends Bundle {
  val ports = Vec(nrPorts, new Port(addrWidth))
}

abstract class MultiCoreDevice(nrPorts: Int, addrWidth: Int) extends Module {
  val io = IO(new MultiPort(nrPorts, addrWidth))
}
