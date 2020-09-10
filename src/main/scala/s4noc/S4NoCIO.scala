/*
  Top level of the S4NOC with a CPU interface.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */

package s4noc

import chisel3._

class S4NoCIO(n: Int, txFifo: Int, rxFifo: Int, width: Int) extends Module  {
  val io = IO(new Bundle {
    val cpuPorts = Vec(n, new CpuPortComb(width))
    val cycCnt = Output(UInt(32.W))
  })

  val s4noc = Module(new S4NoC(n, txFifo, rxFifo, width))

  for (i <- 0 until n) {
    val ci = Module(new CpuInterfaceComb(UInt(width.W), width))
    s4noc.io.networkPort(i) <> ci.io.networkPort
    io.cpuPorts(i) <> ci.io.cpuPort
  }

  val cntReg = RegInit(0.U(32.W))
  cntReg := cntReg + 1.U
  io.cycCnt := cntReg
}

