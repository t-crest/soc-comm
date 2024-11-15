package spi

import chisel.lib.uart._
import chisel3._
import chisel3.util._

import debug._

/**
  * This is the top level to for the UART output and a test blinking LED.
  */
class BitBang(frequ: Int) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val sw = Input(UInt(4.W))
    val led = Output(UInt(4.W))
    val ncs = Output(Bool()) // pmod 4, ic 1
    val miso = Input(Bool()) // pmod 3, ic 2
    val mosi = Output(Bool()) // pmod 2, ic 5
    val sck = Output(Bool()) // pmod 1, ic 6
  })
  io.led := io.sw

  val dbg = Module(new UartDebug(100000000, 115200))

  dbg.io.rx := io.rx
  io.tx := dbg.io.tx

  dbg.io.din := ~dbg.io.dout

  val regVal = RegInit(0.U(3.W))

  regVal := dbg.io.dout(2, 0)
  io.led := io.miso ## dbg.io.dout(2, 0)

  io.sck := regVal(0)
  io.mosi := regVal(1)
  io.ncs := regVal(2)
}

// generate Verilog
object BitBang extends App {
  emitVerilog(new BitBang(100000000),  Array("--target-dir", "generated"))
}