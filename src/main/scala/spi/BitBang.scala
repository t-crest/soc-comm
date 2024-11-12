package spi

import chisel.lib.uart._
import chisel3._
import chisel3.util._

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

  val tx = Module(new BufferedTx(100000000, 115200))
  val rx = Module(new Rx(100000000, 115200))

  io.tx := tx.io.txd
  rx.io.rxd := io.rx

  tx.io.channel.bits := '0'.U + io.miso
  tx.io.channel.valid := rx.io.channel.valid
  rx.io.channel.ready := true.B
  val regVal = RegEnable('0'.U + rx.io.channel.bits(3, 0), rx.io.channel.valid)
  io.led := io.miso ## regVal(2, 0)

  io.sck := regVal(0)
  io.mosi := regVal(1)
  io.ncs := regVal(2)
}

// generate Verilog
object BitBang extends App {
  emitVerilog(new BitBang(100000000),  Array("--target-dir", "generated"))
}