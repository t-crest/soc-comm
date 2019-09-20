/*
  Top level of the S4NOC.
  Interface is in and out FIFOs.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */

package s4noc

import chisel3._

class S4NoC(n: Int, txFifo: Int, rxFifo: Int, width: Int) extends Module  {
  val io = IO(new Bundle {
    val networkPort = Vec(n, new NetworkPort(width))
    val cycCnt = Output(UInt(32.W))
  })

  val dim = math.sqrt(n).toInt
  if (dim * dim != n) throw new Error("Number of cores must be quadratic")

  val net = Module(new Network(dim, UInt(width.W)))

  for (i <- 0 until n) {
    val ni = Module(new NetworkInterface(dim, txFifo, rxFifo, UInt(width.W), width))
    net.io.local(i) <> ni.io.local
    io.networkPort(i) <> ni.io.networkPort
  }

  val cntReg = RegInit(0.U(32.W))
  cntReg := cntReg + 1.U
  io.cycCnt := cntReg
}

object S4NoC extends App {

  chisel3.Driver.execute(Array("--target-dir", "generated"),
    () => new S4NoC(4, 2, 2, 32))
}

