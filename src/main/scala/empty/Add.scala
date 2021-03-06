/*
 * Dummy file to start a Chisel project.
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 */

package empty

import Chisel._

class Add extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(width = 8))
    val b = Input(UInt(width = 8))
    val c = Output(UInt(width = 8))
  })

  val reg = RegInit(UInt(0, width = 8))
  reg := io.a + io.b

  io.c := reg
}

object AddMain extends App {
  println("Generating the adder hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new Add(), Array("--target-dir", "generated"))

}