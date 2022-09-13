/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._
import chisel3.util._
import chisel.lib.fifo._
import soc.ReadyValidChannelsIO

class NetworkInterface[T <: Data](conf: Config, dt: T) extends Module {
  val io = IO(new Bundle {
    val networkPort = Flipped(new ReadyValidChannelsIO(Entry(dt)))
    val local = Flipped(new ChannelIO(dt))
  })

  val len = Schedule(conf.dim).schedule.length

  val regCnt = RegInit(0.U(log2Up(len).W))
  regCnt := Mux(regCnt === (len - 1).U, 0.U, regCnt + 1.U)
  // TDM schedule starts one cycle later for read data delay of OneWayMemory
  // Maybe we can use that delay here as well for something good
  val regTimeSlot = RegNext(regCnt, init = 0.U)

  // in/out direction is from the network view
  // flipped here
  val txFifo = Module(new BubbleFifo(Entry(dt), conf.txDepth))
  io.networkPort.tx <> txFifo.io.enq

  // local buffer to avoid combinational ready/valid
  val txFullReg = RegInit(false.B)
  val txDataReg = Reg(Entry(dt))

  when(!txFullReg && txFifo.io.deq.valid) {
    txDataReg := txFifo.io.deq.bits
    txFullReg := true.B
  }
  txFifo.io.deq.ready := !txFullReg
  io.local.in.data := txDataReg.data
  val doSend = txFullReg && regTimeSlot === txDataReg.time
  io.local.in.valid := doSend
  when (doSend) { txFullReg := false.B }

  val rxFifo = Module(new BubbleFifo(Entry(dt), conf.rxDepth))
  io.networkPort.rx <> rxFifo.io.deq

  // local buffer to avoid combinational ready/valid
  val rxFullReg = RegInit(false.B)
  val rxDataReg = Reg(Entry(dt))

  when (!rxFullReg && io.local.out.valid) {
    rxDataReg.data := io.local.out.data
    rxDataReg.time := regTimeSlot
    rxFullReg := true.B
  }

  rxFifo.io.enq.valid := rxFullReg
  rxFifo.io.enq.bits := rxDataReg
  when (rxFullReg && rxFifo.io.enq.ready) {
    rxFullReg := false.B
  }
}