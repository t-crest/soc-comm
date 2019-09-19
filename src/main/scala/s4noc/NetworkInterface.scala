/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._
import chisel3.util._


class NetworkPort(private val size: Int) extends Bundle {
  val tx = new WriterIO(size)
  val rx = new ReaderIO(size)
}

// This should be a generic for the FIFO
class Entry(private val w: Int) extends Bundle {
  val data = UInt(w.W)
  val time = UInt(8.W)
}

class NetworkInterface[T <: Data](dim: Int, txDepth: Int, rxDepth: Int, dt: T, width: Int) extends Module {
  val io = IO(new Bundle {
    val networkPort = new NetworkPort(width)
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

  val txFifo = Module(new BubbleFifo(txDepth, width))
  io.networkPort.tx <> txFifo.io.enq

  io.local.in.data := txFifo.io.deq.dout.data
  val doDeq = !txFifo.io.deq.empty && regDelay === txFifo.io.deq.dout.time
  io.local.in.valid := doDeq
  txFifo.io.deq.read := doDeq

  val rxFifo = Module(new BubbleFifo(rxDepth, width))
  io.networkPort.rx <> rxFifo.io.deq

  rxFifo.io.enq.write := false.B
  rxFifo.io.enq.din.data := io.local.out.data
  rxFifo.io.enq.din.time := regDelay
  when (io.local.out.valid && !rxFifo.io.enq.full) {
    rxFifo.io.enq.write := true.B
  }
}
