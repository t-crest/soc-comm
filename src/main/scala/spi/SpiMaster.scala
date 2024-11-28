package spi

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum


class SpiIO extends Bundle {
  val ncs = Output(Bool())
  val sclk = Output(Bool())
  val mosi = Output(Bool())
  val miso = Input(Bool())
}

class SpiMaster extends Module {
  val spi = IO(new SpiIO)

  val io = IO(new Bundle {
    val readAddr = Flipped(Decoupled(UInt(24.W)))
    val readData = Decoupled(UInt(32.W))
  })

  object State extends ChiselEnum {
    val start, idle, tx1, tx2, rx1, rx2, done1, done2 = Value
  }
  import State._
  val state = RegInit(idle)

  val mosiReg = RegInit(0.U(32.W))
  val misoReg = RegInit(0.U(32.W))
  val bitsReg = RegInit(0.U(8.W))
  val cntReg = RegInit(0.U(32.W))
  val CNT_MAX = 1.U


  // TODO: should those signals be in a register? Probably better timing
  // at least SCLK
  spi.ncs := 1.U
  spi.sclk := 0.U
  spi.mosi := mosiReg(31)
  // little endian return
  io.readData.bits := misoReg(7, 0) ## misoReg(15, 8) ## misoReg(23, 16) ## misoReg(31, 24)
  io.readData.valid := false.B
  io.readAddr.ready := false.B

  val JTAG_ID = 0x9f.U
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
        io.readAddr.ready := true.B
        when(io.readAddr.valid) {
          state := tx1
          bitsReg := 31.U
          cntReg := 0.U
          mosiReg := READ ## io.readAddr.bits
        }
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
        mosiReg := mosiReg << 1
        bitsReg := bitsReg - 1.U
        when(bitsReg === 0.U) {
          state := rx1
          bitsReg := 31.U
        }
      }
    }
    // maybe one tick earlier
    is(rx1) {
      spi.ncs := 0.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        misoReg := misoReg(30, 0) ## spi.miso
        state := rx2
        cntReg := 0.U
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
        when(bitsReg === 0.U) {
          state := done1
        }
      }
    }
    is(done1) {
      spi.ncs := 0.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      when(cntReg === CNT_MAX) {
        state := done2
        cntReg := 0.U
      }
    }
    is(done2) {
      spi.ncs := 1.U
      spi.sclk := 0.U
      cntReg := cntReg + 1.U
      io.readData.valid := true.B
      when(io.readData.ready) {
        state := idle
      }
    }
  }
}

object SpiMaster extends App {
  emitVerilog(new SpiMaster(), Array("--target-dir", "generated"))
}
