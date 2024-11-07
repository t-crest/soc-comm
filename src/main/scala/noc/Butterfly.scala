package noc

import chisel3._
import chisel3.util._

/*
  * Example Butterfly NoC from Dally's Chapter 2
  */

object Const {
  val H = 3.U
  val P = 2.U
  val N = 0.U
}

class ButterChannel extends Bundle {
  val typ = UInt(2.W)
  val data = UInt(16.W)
}

class ButterPort extends Bundle {
  val port = Input(Vec(4, new ButterChannel()))
}

class ButterflyRouter extends Module {
  val io = IO(new Bundle {
    val inPort = new ButterPort()
    val outPort = Flipped(new ButterPort())
  })

  // This is not super elegant
  val inReg = new Array[Data](4)
  val outReg = new Array[Data](4)

  val muxes = Array.fill(4)(Wire(Vec(4, new ButterChannel)))

  val resetVal = Wire((new ButterChannel()))
  resetVal.typ := Const.N
  resetVal.data := 0.U

  for (i <- 0 until 4) {
    inReg(i) = RegNext(io.inPort.port(i), resetVal)
  }

  for (i <- 0 until 4) {
    for (j <- 0 until 4) {
      muxes(i)(j) := inReg(j)
    }
    val sel = i.U
    outReg(i) = RegNext(muxes(i)(sel), resetVal)
    io.outPort.port(i) := outReg(i)
  }
}


class Butterfly extends Module {
  val io = IO(new Bundle {
    val inPorts = Vec(16, new ButterPort())
    val outPorts = Flipped(Vec(16, new ButterPort()))
  })

  val stage1 = Array.fill(16)(Module(new ButterflyRouter()))
  val stage2 = Array.fill(16)(Module(new ButterflyRouter()))
  val stage3 = Array.fill(16)(Module(new ButterflyRouter()))


  for (i <- 0 until 16) {
    io.inPorts(i) <> stage1(i).io.inPort
    io.outPorts(i) <> stage3(i).io.outPort
  }



  for (i <- 0 until 4) {
    for (j <- 0 until 4) {
      for (k <- 0 until 4) {
        stage1(i*4+j).io.outPort.port(k) <> stage2(k*4+j).io.inPort.port(i)
        stage2(i*4+j).io.outPort.port(k) <> stage3(i*4+k).io.inPort.port(j)
      }
    }
  }
}

object Butterfly extends App {
  emitVerilog(new Butterfly(), Array("--target-dir", "generated"))
}
