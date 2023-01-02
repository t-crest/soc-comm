/*
  Top level of the S4NOC.
  Interface is in and out FIFOs.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */

package s4noc

import chisel3._
import soc.ReadyValidChannelsIO

/**
  * Top level of the S4NOC.
  * Interface is in and out FIFOs with data and time/core.
  *
  * @param conf
  */
class S4NoC(conf: Config) extends Module  {
  val io = IO(Vec(conf.n, new ChannelIO(UInt(conf.width.W))))

  val net = Module(new Network(conf.dim, UInt(conf.width.W)))

  for (i <- 0 until conf.n) {
    net.io.local <> io
  }
}

object S4NoC extends App {
  val conf = Config(4, 2, 2, 2, 32)
  emitVerilog(new S4NoC(conf), Array("--target-dir", "generated"))
}

