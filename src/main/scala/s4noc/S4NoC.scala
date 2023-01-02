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
  val io = IO(new Bundle {
    val channels = Vec(conf.n, new ChannelIO(UInt(conf.width.W)))
    val read = Input(Vec(conf.n, new Bool()))
  })

  val net = Module(new Network(conf.dim, UInt(conf.width.W)))
  val bufferedData = Reg(Vec(conf.n, UInt(conf.width.W)))
  val bufferedValid = RegInit(VecInit(Array.fill(conf.n)(false.B)))

  for (i <- 0 until conf.n) {
    bufferedData(i) := Mux(net.io(i).out.valid, net.io(i).out.data, bufferedData(i))
    bufferedValid(i) := (bufferedValid(i) && !io.read(i)) || net.io(i).out.valid
    net.io(i).in <> io.channels(i).in
    io.channels(i).out.data := bufferedData(i)
    io.channels(i).out.valid := bufferedValid(i)
  }
}

object S4NoC extends App {
  val conf = Config(4, 2, 2, 2, 32)
  emitVerilog(new S4NoC(conf), Array("--target-dir", "generated"))
}

