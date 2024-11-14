package debug

import chisel.lib.uart._
import chisel3._
import chisel3.util._

/**
  * Poor mans debugger, using a UART instead of JTAG.
  */
class UartDebug(frequ: Int, baudRate: Int = 115200) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val dout = Output(UInt(32.W))
    val din = Input(UInt(32.W))
  })

  val tx = Module(new BufferedTx(100000000, baudRate))
  val rx = Module(new Rx(100000000, baudRate))

  io.tx := tx.io.txd
  rx.io.rxd := io.rx

  tx.io.channel.bits := rx.io.channel.bits + 1.U
  tx.io.channel.valid := rx.io.channel.valid
  rx.io.channel.ready := tx.io.channel.ready

  io.dout := 0x1234.U
  when (tx.io.channel.valid && tx.io.channel.ready) {
    io.dout := tx.io.channel.bits
  }
}

object UartDebug extends App {
  emitVerilog(new UartDebug(100000000),  Array("--target-dir", "generated"))
}