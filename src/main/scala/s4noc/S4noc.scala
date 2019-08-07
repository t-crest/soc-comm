/*
  Top level of the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */

package s4noc

import chisel3._

class S4noc(n: Int, txFifo: Int, rxFifo: Int, width: Int) extends Module  {
  val io = new Bundle {
    val cpuPorts = Vec(n, new CpuPort(width))
  }

  val dim = math.sqrt(n).toInt
  if (dim * dim != n) throw new Error("Number of cores must be quadratic")

  val net = Module(new Network(dim, UInt(width.W)))

  for (i <- 0 until n) {
    val ni = Module(new NetworkInterface(dim, txFifo, rxFifo, UInt(width.W), width))
    net.io.local(i).in := ni.io.local.out
    ni.io.local.in := net.io.local(i).out
    io.cpuPorts(i) <> ni.io.cpuPort
  }
}

