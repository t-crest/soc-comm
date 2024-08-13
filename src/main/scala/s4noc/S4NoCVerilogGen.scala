/*
  Dummy traffic generator for synthesize results of a stand alone S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._

class S4NoCVerilogGen(conf: Config) extends Module {

  val s4noc = Module(new S4NoCTop(conf))
  // This is almost Chisel 3 syntax.
  val io = IO(new Bundle {
    val data = Output(UInt(conf.width.W))
  })

  val outReg = Array.fill(conf.n) { RegInit(0.U(conf.width.W)) }

  for (i <- 0 until conf.n) {

    val cntReg = RegInit((i*7+5).U((conf.width+8).W))
    cntReg := cntReg + 1.U

    // addresses are in words
    s4noc.io.cpuPorts(i).address := cntReg(7, 2)
    s4noc.io.cpuPorts(i).wrData := cntReg(conf.width+7, 8)
    s4noc.io.cpuPorts(i).wr := cntReg(0)
    s4noc.io.cpuPorts(i).wrMask := 0xf.U
    s4noc.io.cpuPorts(i).rd := cntReg(1)
    // Have some registers before or reduce
    if (i == 0) {
      outReg(i) := RegNext(s4noc.io.cpuPorts(i).rdData)
    } else {
      outReg(i) := RegNext(RegNext(s4noc.io.cpuPorts(i).rdData) | outReg(i-1))
    }
  }

  // Have some registers before hitting the output pins
  // io.data := RegNext(RegNext(RegNext(RegNext((outReg.reduce((x, y) => x | y))))))
  io.data := RegNext(outReg(conf.n-1))
}

object S4NoCVerilogGen extends App {
  println("Generating the S4NoC hardware with a dummy traffic generator")
  emitVerilog(new S4NoCVerilogGen(Config(args(0).toInt, BubbleType(8), BubbleType(8), BubbleType(8), 32)), Array("--target-dir", "generated"))
}