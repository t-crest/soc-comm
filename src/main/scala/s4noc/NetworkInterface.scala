/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import Chisel._

class CpuPort(val w: Int) extends Bundle {
  val addr = UInt(width = 8).asInput
  val rdData = UInt(width = w).asOutput
  val wrData = UInt(width = w).asInput
  val rd = Bool().asInput
  val wr = Bool().asInput

  // The following was not needed in Chisel 2, but now in Chisel 3
  // the issue is the width parameter, what is the right Chisel 3 idiom for this?
    /*
  override def cloneType() = {
    val res = new CpuPort(width)
    res.asInstanceOf[this.type]
  }

     */
}

// This should be a generic for the FIFO
class Entry(width: Int) extends Bundle {
  val data = UInt(width = width).asOutput
  val time = UInt(width = 8).asInput

  override def cloneType() = {
    val res = new Entry(width)
    res.asInstanceOf[this.type]
  }
}



class NetworkInterface[T <: Data](dim: Int, txFifo: Int, rxFifo: Int, dt: T, width: Int) extends Module {
  val io = new Bundle {
    val cpuPort = new CpuPort(width)
    val local = new Channel(dt)
  }

  // TODO: too much repetition
  // Either provide the schedule as parameter
  // or simply read out the TDM counter from the router.
  // Why duplicating it? Does it matter?
  val len = Schedule.getSchedule(dim)._1.length

  val regCnt = Reg(init = UInt(0, log2Up(len)))
  regCnt := Mux(regCnt === UInt(len - 1), UInt(0), regCnt + UInt(1))
  // TDM schedule starts one cycles later for read data delay of OneWayMemory
  // Maybe we can use that delay here as well for something good
  val regDelay = RegNext(regCnt, init = UInt(0))


  val entryReg = Reg(new Entry(width))
  when(io.cpuPort.wr) {
    entryReg.data := io.cpuPort.wrData
    entryReg.time := io.cpuPort.addr
  }

  val inFifo = Module(new BubbleFifo(rxFifo, width))
  inFifo.io.enq.write := Bool(false)
  inFifo.io.enq.din.data := io.cpuPort.wrData
  inFifo.io.enq.din.time := io.cpuPort.addr
  when (io.cpuPort.wr && !inFifo.io.enq.full) {
    inFifo.io.enq.write := Bool(true)
  }

  io.local.out.data := inFifo.io.deq.dout.data
  val doDeq = !inFifo.io.deq.empty && regDelay === inFifo.io.deq.dout.time
  io.local.out.valid := doDeq
  inFifo.io.deq.read := doDeq

  // TODO: what is the rd timing? Same clock cycle or next clock cycle?
  // Maybe we should do the AXI interface for all future designs
  // In our OCP interface we have defined it...

  // for now same clock cycle

  val outFifo = Module(new BubbleFifo(txFifo, width))
  outFifo.io.enq.write := Bool(false)
  outFifo.io.enq.din.data := io.local.in.data
  outFifo.io.enq.din.time := regDelay
  when (io.local.in.valid && !outFifo.io.enq.full) {
    outFifo.io.enq.write := Bool(true)
  }

  io.cpuPort.rdData := outFifo.io.deq.dout.data
  outFifo.io.deq.read := Bool(false)
  val regTime = RegInit(UInt(0, 6))
  when (io.cpuPort.rd) {
    val addr = io.cpuPort.addr
    when (addr === UInt(0))  {
      outFifo.io.deq.read := Bool(true)
    } .elsewhen(addr === UInt(1)) {
      io.cpuPort.rdData := regTime
    } .elsewhen(addr === UInt(2)) {
      io.cpuPort.rdData := Cat(UInt(0, 31), !inFifo.io.enq.full)
    } .elsewhen(addr === UInt(3)) {
      io.cpuPort.rdData := Cat(UInt(0, 31), !outFifo.io.deq.empty)
    }
  }
}
