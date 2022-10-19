package s4noc

import chisel3._
import chisel3.util._
import chisel.lib.fifo._
import soc.ReadyValidChannelsIO

// TODO: make this (better) configurable

/**
  * Network interface (NI) for the S4NOC.
  * Contains various FIFO buffers.
  * Expects insertion of data with the time slot to indicate the destination.
  * // TODO: do we want this? Or would core id be nicer?
  * We can do the mapping in the CPU interface, but this is not nice.
  *
  * @param id
  * @param conf
  * @param dt
  * @tparam T
  */
class NetworkInterface[T <: Data](id: Int, conf: Config, dt: T) extends Module {
  val io = IO(new Bundle {
    val networkPort = Flipped(new ReadyValidChannelsIO(Entry(dt)))
    val local = Flipped(new ChannelIO(dt))
  })

  val sched = Schedule(conf.dim)
  val len = sched.schedule.length
  // from slot count to destination core
  val translationTableSend = VecInit(Seq.fill(len)(0.U(8.W)))
  // from slot count to sending core
  val translationTableRcv = VecInit(Seq.fill(len)(0.U(8.W)))
  val validSlot = VecInit(Seq.fill(len)(false.B))
  for (i <- 0 until len) {
    val dest = sched.timeToDest(id, i).dest
    if (dest != -1) {
      translationTableSend(i) := dest.U
      validSlot(i) := true.B
    }
    val src = sched.timeToSource(id, i)
    if (src != -1) {
      translationTableRcv(i) := src.U
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

  // val toCore = translationTable(txFifo.io.deq.bits.core)
  val toCore = txFifo.io.deq.bits.core

  // TODO: Minimum should be a single register. Could be enough in most cases.
  val splitBuffers = (0 until conf.n).map(_ => Module(new MemFifo(dt, conf.splitDepth)))
  for (i <- 0 until conf.n) {
    splitBuffers(i).io.enq.bits := txFifo.io.deq.bits.data
  }

  // there must be a more elegant solution
  val enqReadyVec = VecInit(Seq.fill(conf.n)(false.B))
  val enqValidVec = VecInit(Seq.fill(conf.n)(false.B))
  val deqValidVec = VecInit(Seq.fill(conf.n)(false.B))
  val deqDataVec = Wire(Vec(conf.n, dt))
  val deqReadyVec = VecInit(Seq.fill(conf.n)(false.B))

  // connections
  for (i <- 0 until conf.n) {
    enqReadyVec(i) := splitBuffers(i).io.enq.ready
    splitBuffers(i).io.enq.valid := enqValidVec(i)
    deqValidVec(i) := splitBuffers(i).io.deq.valid
    deqDataVec(i) := splitBuffers(i).io.deq.bits
    splitBuffers(i).io.deq.ready := deqReadyVec(i)
  }
  // the following is a combinational valid/ready path from a split buffer
  txFifo.io.deq.ready := enqReadyVec(toCore)
  when (txFifo.io.deq.valid) {
    when (enqReadyVec(toCore)) {
      enqValidVec(toCore) := true.B
    }
  }

  val core = translationTableSend(timeSlotReg)
  val slotOk = validSlot(timeSlotReg)
  when (slotOk) { deqReadyVec(core) := true.B }

  val valid = deqValidVec(core) && slotOk
  io.local.in.data := deqDataVec(core)
  io.local.in.valid := valid

  // RX
  val rxFifo = Module(new DoubleBufferFifo(Entry(dt), conf.rxDepth))
  // rxFifo.io.enq.ready is ignored. When the FIFO is full, packets are simply dropped.
  dontTouch(rxFifo.io.enq.ready)
  rxFifo.io.enq.valid := io.local.out.valid
  rxFifo.io.enq.bits.data := io.local.out.data
  rxFifo.io.enq.bits.core := translationTableRcv(timeSlotReg)

  io.networkPort.rx <> rxFifo.io.deq
}