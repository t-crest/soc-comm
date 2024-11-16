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
    val led = Output(UInt(16.W))
    val accell = new SpiIO()
    val flash = new SpiIO()
    val sram = new SpiIO()
    /*
    val ncs = Output(Bool()) // pmod 4, ic 1
    val miso = Input(Bool()) // pmod 3, ic 2
    val mosi = Output(Bool()) // pmod 2, ic 5
    val sck = Output(Bool()) // pmod 1, ic 6

     */
  })
  val dbg = Module(new UartDebug(100000000, 115200))

  dbg.io.rx := io.rx
  io.tx := dbg.io.tx



  val valReg = RegInit(0.U(32.W))
  valReg := dbg.io.dout

  io.led := io.sram.miso ## valReg(10, 8) ## io.flash.miso ## valReg(6, 4) ## io.accell.miso ## valReg(2, 0)

  io.accell.sclk := valReg(0)
  io.accell.mosi := valReg(1)
  io.accell.ncs := valReg(2)

  io.flash.sclk := valReg(4)
  io.flash.mosi := valReg(5)
  io.flash.ncs := valReg(6)

  io.sram.sclk := valReg(8)
  io.sram.mosi := valReg(9)
  io.sram.ncs := valReg(10)

  dbg.io.din := io.sram.miso ## 0.U(3.W) ## io.flash.miso ## 0.U(3.W) ## io.accell.miso ## 0.U(3.W)
}

// generate Verilog
object BitBang extends App {
  emitVerilog(new BitBang(100000000),  Array("--target-dir", "generated"))
}