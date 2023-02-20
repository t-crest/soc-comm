/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel.lib.fifo._
import chisel3._
import chisel3.util._
import soc.ReadyValidChannelsIO

// TODO: probably make the other NI configurable. Or maybe drop the single one
// TODO: if kept it needs to switch to core id
class NetworkInterfaceSingle[T <: Data](id: Int, conf: Config, dt: T) extends Module {
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

  // in/out direction is from the network view
  // flipped here
  val txFifo = conf.tx.getFifo(dt)
  io.networkPort.tx <> txFifo.io.enq

  val doSend = txFifo.io.deq.valid && translationTableSend(timeSlotReg) === txFifo.io.deq.bits.core
  io.local.in.valid := doSend
  txFifo.io.deq.ready := doSend
  io.local.in.data := txFifo.io.deq.bits.data

  // RX
  val rxFifo = conf.rx.getFifo(dt)
  // rxFifo.io.enq.ready is ignored. When the FIFO is full, packets are simply dropped.
  rxFifo.io.enq.valid := io.local.out.valid
  rxFifo.io.enq.bits.data := io.local.out.data
  rxFifo.io.enq.bits.core := translationTableRcv(timeSlotReg)

  io.networkPort.rx <> rxFifo.io.deq

}