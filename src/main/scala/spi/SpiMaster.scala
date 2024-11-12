package spi

import chisel3._
import chisel3.util._
class SpiMaster extends Module {
  val spi = IO (new Bundle {
    val ncs = Output(Bool())
    val sclk = Output(Bool())
    val mosi = Output(Bool())
    val miso = Input(Bool())
  })
  val io = IO(new Bundle {
    val dataOut = Output(UInt(8.W))
    val dataIn = Input(UInt(8.W))
    val dataValid = Input(Bool())
  })

  object State extends ChiselEnum {
    val start, idle, tx1, tx2, rx1, rx2 = Value
  }
  import State._
  val state = RegInit(idle)

  val mosiReg = RegInit(0.U(8.W))
  val misoReg = RegInit(0.U(8.W))
  val bitsReg = RegInit(0.U(8.W))
  val cntReg = RegInit(0.U(32.W))
  val CNT_MAX = 9000.U


  spi.ncs := 1.U
  spi.sclk := 0.U
  spi.mosi := mosiReg(7)
  io.dataOut := misoReg

  // val JTAG_ID = 0x90.U
  val JTAG_ID = 0xab.U
  val RD_STATUS = 0x05.U
  val RDSR = 0x05.U
  val READ = 0x03.U

  switch(state) {
    is(start) {
      spi.ncs := 1.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := idle
        cntReg := 0.U
      }
    }
    is(idle) {
      spi.ncs := 1.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        // when data is available
        state := tx1
        bitsReg := 7.U
        cntReg := 0.U
        mosiReg := JTAG_ID
      }

    }
    is(tx1) {
      spi.ncs := 0.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := tx2
        cntReg := 0.U
      }
    }
    is(tx2) {
      spi.ncs := 0.U
      spi.sclk := 1.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := tx1
        cntReg := 0.U
        mosiReg := mosiReg(6, 0) ## 0.U // io.dataIn(7))
        bitsReg := bitsReg - 1.U
        when(bitsReg === 0.U) {
          state := rx1
        }
      }
    }
    is(rx1) {
      spi.ncs := 0.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := rx2
        cntReg := 0.U
        bitsReg := bitsReg - 1.U
        when(bitsReg === 0.U) {
          state := rx1
        }
      }
    }
    is(rx2) {
      spi.ncs := 0.U
      spi.sclk := 1.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := rx1
        cntReg := 0.U
        bitsReg := bitsReg - 1.U
        // probably wrong bit order
        misoReg := spi.miso ## misoReg(7, 1)
        when(bitsReg === 0.U) {
          state := idle
        }
      }
    }
  }
}

object SpiMaster extends App {
  emitVerilog(new SpiMaster(), Array("--target-dir", "generated"))
}
