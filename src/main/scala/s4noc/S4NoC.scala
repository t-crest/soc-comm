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
  * Interface is in and out FIFOs with data and core id.
  *
  * @param conf
  */
class S4NoC(conf: Config) extends Module  {
  val io = IO(new Bundle {
    val networkPort = Vec(conf.n, Flipped(new ReadyValidChannelsIO(Entry(UInt(conf.width.W)))))
  })


  val net = Module(new Network(conf.dim, UInt(conf.width.W)))

  for (i <- 0 until conf.n) {
    // can use NetworkInterfaceSingle for paper numbers
    val ni = Module(new NetworkInterface(i, conf, UInt(conf.width.W)))
    net.io.local(i) <> ni.io.local
    io.networkPort(i) <> ni.io.networkPort
  }
}

object S4NoC extends App {
  val conf = Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32)
  emitVerilog(new S4NoC(conf), Array("--target-dir", "generated"))
}

