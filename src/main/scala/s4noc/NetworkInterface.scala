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

// TODO: add channel buffers
// TODO: make this configurable
class NetworkInterface[T <: Data](id: Int, conf: Config, dt: T) extends Module {
  val io = IO(new Bundle {
    val networkPort = Flipped(new ReadyValidChannelsIO(Entry(dt)))
    val local = Flipped(new ChannelIO(dt))
  })

  val sched = Schedule(conf.dim)
  val len = sched.schedule.length
  // from slot count to destination core
  val translationTable = VecInit(Seq.fill(len)(0.U(8.W)))
  val validSlot = VecInit(Seq.fill(len)(false.B))
  for (i <- 0 until len) {
    val dest = sched.timeToDest(0, i)._1
    if (dest != -1) {
      translationTable(i) := dest.U
      validSlot(i) := true.B
    }
  }

  val regCnt = RegInit(0.U(log2Up(len).W))
  regCnt := Mux(regCnt === (len - 1).U, 0.U, regCnt + 1.U)
  // TDM schedule starts one cycle later for read data delay of OneWayMemory
  // Maybe we can use that delay here as well for something good
  val timeSlotReg = RegNext(regCnt, init = 0.U)

  // TX
  // in/out direction is from the network view
  // flipped here
  val txFifo = Module(new MemFifo(Entry(dt), conf.txDepth))
  io.networkPort.tx <> txFifo.io.enq

  // local buffer to avoid combinational ready/valid
  val txFullReg = RegInit(false.B)
  val txDataReg = Reg(Entry(dt))

  when(!txFullReg && txFifo.io.deq.valid) {
    txDataReg := txFifo.io.deq.bits
    txFullReg := true.B
  }
  txFifo.io.deq.ready := !txFullReg

  val splitBuffers = (0 until conf.n).map(_ => Module(new BubbleFifo(Entry(dt), 8)))
  for (i <- 0 until conf.n) {
    splitBuffers(i).io.enq.bits := txDataReg
  }

  // there must be a more elegant solution
  val enqReadyVec = VecInit(Seq.fill(conf.n)(false.B))
  val enqValidVec = VecInit(Seq.fill(conf.n)(false.B))
  val deqValidVec = VecInit(Seq.fill(conf.n)(false.B))
  val deqDataVec = Wire(Vec(conf.n, Entry(dt)))
  val deqReadyVec = VecInit(Seq.fill(conf.n)(false.B))

  // connections
  for (i <- 0 until conf.n) {
    enqReadyVec(i) := splitBuffers(i).io.enq.ready
    splitBuffers(i).io.enq.valid := enqValidVec(i)
    deqValidVec(i) := splitBuffers(i).io.deq.valid
    deqDataVec(i) := splitBuffers(i).io.deq.bits
    splitBuffers(i).io.deq.ready := deqReadyVec(i)
  }

  when (txFullReg) {
    when (enqReadyVec(translationTable(txDataReg.time))) {
      enqValidVec(translationTable(txDataReg.time)) := true.B
      txFullReg := false.B
    }
  }

  val core = translationTable(timeSlotReg)
  val slotOk = validSlot(timeSlotReg)
  when (slotOk) { deqReadyVec(core) := true.B }

  val valid = deqValidVec(core) && slotOk
  io.local.in.data := deqDataVec(core).data
  io.local.in.valid := valid

  // RX
  val rxFifo = Module(new MemFifo(Entry(dt), conf.rxDepth))
  io.networkPort.rx <> rxFifo.io.deq

  // local buffer to avoid combinational ready/valid
  val rxFullReg = RegInit(false.B)
  val rxDataReg = Reg(Entry(dt))

  when (!rxFullReg && io.local.out.valid) {
    rxDataReg.data := io.local.out.data
    rxDataReg.time := timeSlotReg
    rxFullReg := true.B
  }

  rxFifo.io.enq.valid := rxFullReg
  rxFifo.io.enq.bits := rxDataReg
  when (rxFullReg && rxFifo.io.enq.ready) {
    rxFullReg := false.B
  }
}