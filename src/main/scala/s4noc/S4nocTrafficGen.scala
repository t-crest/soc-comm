/*
  Traffic generator for synthesize results of a stand alone S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._

class S4nocTrafficGen(nrNodes: Int, txFifo: Int, rxFifo: Int, width: Int) extends Module {

  val s4noc = Module(new S4noc(nrNodes, txFifo, rxFifo, width))
  // This is almost Chisel 3 syntax.
  val io = IO(new Bundle {
    val data = Output(UInt(width.W))
  })

  val outReg = Array.fill(nrNodes) { RegInit(0.U(width.W)) }

  for (i <- 0 until nrNodes) {

    val cntReg = RegInit((i*7+5).U((width+8).W))
    cntReg := cntReg + 1.U

    // addresses are in words
    s4noc.io.cpuPorts(i).addr := cntReg(7, 2)
    s4noc.io.cpuPorts(i).wrData := cntReg(width+7, 8)
    s4noc.io.cpuPorts(i).wr := cntReg(0)
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
  io.data := RegNext(outReg(nrNodes-1))
}

object S4nocTrafficGen extends App {
  println("Generating the S4NoC hardware with a traffic generator")
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new S4nocTrafficGen(args(0).toInt, 8, 8, 32))
}