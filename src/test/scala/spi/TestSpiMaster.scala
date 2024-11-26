package spi

/*
 * Test SPI.
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 */

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class s25hs512t extends HasBlackBoxPath {
  val io = IO(new Bundle() {
    val SI = Input(Bool())
    val SO = Output(Bool())
    val SCK = Input(Bool())
    val CSNeg = Input(Bool())
    val WPNeg = Input(Bool())
    val RESETNeg = Input(Bool())
    val IO3_RESETNeg = Input(Bool())
  }
  )
  addPath("./src/test/verilog/s25hs512t.sv")
}


class s25fl256s extends HasBlackBoxPath {
  val io = IO(new Bundle() {
    val SI = Input(Bool())
    val SO = Output(Bool())
    val SCK = Input(Bool())
    val RSTNeg = Input(Bool())
    val CSNeg = Input(Bool())
    val WPNeg = Input(Bool())
    val HOLDNeg = Input(Bool())
  }
  )
  addPath("./src/test/verilog/s25fl256s.v")
}


class TopTest extends Module {
  val io = IO(new Bundle {
    val dout = Output(UInt(8.W))
  })

  val ctrl = Module(new SpiMaster)
  /*
  val flash = Module(new s25hs512t)
  flash.io.SI := 0.U
  flash.io.SCK := ctrl.spi.sclk
  flash.io.CSNeg := false.B
  flash.io.WPNeg := true.B
  flash.io.RESETNeg := true.B // probably !reset
  flash.io.IO3_RESETNeg := true.B
   */

  val flash = Module(new s25fl256s)
  flash.io.SI := ctrl.spi.mosi
  flash.io.SCK := ctrl.spi.sclk
  flash.io.CSNeg := ctrl.spi.ncs
  flash.io.RSTNeg := !reset.asUInt
  flash.io.WPNeg := true.B
  flash.io.HOLDNeg := true.B
  ctrl.spi.miso := flash.io.SO

  ctrl.io.readAddr.bits := 0.U
  ctrl.io.readAddr.valid := false.B

  io.dout := ctrl.io.readData.bits
}
class TestSpiMaster(doNotRun: String) extends AnyFlatSpec with ChiselScalatestTester {

  /*
  "SpiMaster" should "work" in {
    test(new SpiMaster).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(20)
    }
  }

   */

  "TopTest" should "work" in {
    test(new TopTest).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { dut =>
      dut.clock.setTimeout(10001)
      dut.clock.step(10000)
    }
  }
}
