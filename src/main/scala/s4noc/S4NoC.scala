/*
  Top level of the S4NOC.
  Interface is in and out FIFOs.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */

package s4noc

import chisel3._
import chisel3.util._
import soc.ReadyValidChannelsIO

/**
  * Top level of the S4NOC.
  * Interface is in and out FIFOs with data and time/core.
  *
  * @param conf
  */
class S4NoC(conf: Config) extends Module  {
  val io = IO(Vec(conf.n, new CpuNocIO(UInt(conf.width.W), conf)))

  val net = Module(new Network(conf.dim, UInt(conf.width.W)))

  for (i <- 0 until conf.n) {
    val ni = Module(new NetworkInterface(i, conf, UInt(conf.width.W)))
    net.io.local(i) <> ni.io.network
    io(i) <> ni.io.cpu
  }
}

object S4NoC extends App {
  val conf = Config(4, 2, 2, 2, 32)
  emitVerilog(new S4NoC(conf), Array("--target-dir", "generated"))
}

