/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._
import chisel3.util._

import chisel.lib.fifo._


class NetworkPort[T <: Data](private val dt: T) extends Bundle {
  val tx = Flipped(new DecoupledIO(new Entry(dt)))
  val rx = new DecoupledIO(new Entry(dt))
}

// This should be a generic data for the FIFO
class Entry[T <: Data](private val dt: T) extends Bundle {
  val data = dt.cloneType
  val time = UInt(8.W)

  // TODO: why is this apply not working?
  // Why is the return value Unit?
  // It is Entry, now...
  def apply(dt: T) = {
    new Entry(dt)
  }
}

class NetworkInterface[T <: Data](dim: Int, txDepth: Int, rxDepth: Int, dt: T) extends Module {
  val io = IO(new Bundle {
    val networkPort = new NetworkPort(dt)
    val local = Flipped(new Channel(dt))
  })

  // TODO: too much repetition
  // Either provide the schedule as parameter
  // or simply read out the TDM counter from the router.
  // Why duplicating it? But does it matter?
  val len = Schedule.getSchedule(dim)._1.length

  val regCnt = RegInit(0.U(log2Up(len).W))
  regCnt := Mux(regCnt === (len - 1).U, 0.U, regCnt + 1.U)
  // TDM schedule starts one cycles later for read data delay of OneWayMemory
  // Maybe we can use that delay here as well for something good
  val regDelay = RegNext(regCnt, init = 0.U)

  // in/out direction is from the network view
  // flipped here
  val txFifo = Module(new BubbleFifo(new Entry(dt), txDepth))
  io.networkPort.tx <> txFifo.io.enq

  io.local.in.data := txFifo.io.deq.bits.data
  val doDeq = txFifo.io.deq.valid && regDelay === txFifo.io.deq.bits.time
  io.local.in.valid := doDeq
  // TODO: this is a combinational ready. We do not want this
  // Solution: generate ready just from right timing
  txFifo.io.deq.ready := doDeq

  val rxFifo = Module(new BubbleFifo(new Entry(dt), rxDepth))
  io.networkPort.rx <> rxFifo.io.deq

  rxFifo.io.enq.valid := false.B
  rxFifo.io.enq.bits.data := io.local.out.data
  rxFifo.io.enq.bits.time := regDelay
  // TODO: again a combinational ready/valid
  when (io.local.out.valid && rxFifo.io.enq.ready) {
    rxFifo.io.enq.valid := true.B
  }
}