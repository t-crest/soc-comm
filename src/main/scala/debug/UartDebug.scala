package debug

import chisel.lib.uart._
import chisel3._
import chisel3.util._

/**
  * This is the top level to for the UART output and a test blinking LED.
  */
class UartDebug(frequ: Int) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val dout = Output(UInt(32.W))
    val din = Input(UInt(32.W))
  })

  val tx = Module(new BufferedTx(100000000, 115200))
  val rx = Module(new Rx(100000000, 115200))

  io.tx := tx.io.txd
  rx.io.rxd := io.rx


}

// generate Verilog
object UartDebug extends App {
  emitVerilog(new UartDebug(100000000),  Array("--target-dir", "generated"))
}