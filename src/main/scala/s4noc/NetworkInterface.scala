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
    val cpu = new CpuNocIO(dt, conf)
    val network = Flipped(new ChannelIO(dt))
  })
  val readingValidInsteadOfData: Bool = io.cpu.loadFromCore(log2Ceil(conf.n))
  val readingRegCnt: Bool = io.cpu.loadFromCore(log2Ceil(conf.n) + 1)
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

  val toCore = translationTableSend(timeSlotReg)
  val fromCore = translationTableRcv(timeSlotReg)

  val receivedData = Reg(Vec(conf.n, new SingleChannelIO(dt)))
  when (io.network.out.valid) {
    receivedData(fromCore).data := io.network.out.data
    receivedData(fromCore).valid := true.B
  }
  when (io.cpu.load.ready && !readingValidInsteadOfData) {
    receivedData(io.cpu.loadFromCore).valid := false.B
  }
  io.cpu.load.bits := Mux(
    readingRegCnt,
    regCnt,
    Mux(
      readingValidInsteadOfData,
      receivedData(io.cpu.loadFromCore(1, 0)).valid,
      receivedData(io.cpu.loadFromCore(1, 0)).data
    )
  )
  io.network.in.valid := io.cpu.store.valid
  io.network.in.data := io.cpu.store.bits
}
