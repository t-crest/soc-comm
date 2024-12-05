package s4noc

import chisel3._
import soc._


/**
  * Top level of the S4NOC with PipeCon CPU interfaces.
  *
  * Author: Martin Schoeberl (martin@jopdesign.com)
  * license see LICENSE
  * @param conf
  */
class S4NoCTop(conf: Config) extends Module  {
  val io = IO(new Bundle {
    val cpuPorts = Vec(conf.n, new PipeConIO(conf.width))
    val cycCnt = Output(UInt(32.W))
  })

  val s4noc = Module(new S4NoC(conf))
  for (i <- 0 until conf.n) {
    val ci = Module(new PipeConS4NoC(conf.width, Entry(UInt(conf.width.W))))
    s4noc.io.networkPort(i) <> ci.rv
    io.cpuPorts(i) <> ci.cpuPort
  }

  val cntReg = RegInit(0.U(32.W))
  cntReg := cntReg + 1.U
  io.cycCnt := cntReg
}

