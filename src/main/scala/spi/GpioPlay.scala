package spi

import chisel3._
class GpioPlay extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(4.W))
    val led = Output(UInt(8.W))
  })

 io.led := io.din

}

object GpioPlay extends App {
  emitVerilog(new GpioPlay(), Array("--target-dir", "generated"))
}
