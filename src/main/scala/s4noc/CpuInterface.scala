/*
  Network interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._
import chisel3.util._

/**
  * A simple processor interface. No handshake.
  * Read timing is currently same clock cycle (combinational read).
  * In Patmos the usage of the read data is delayed by one clock cycle in the OCP wrapper.
  *
  * NOTE: this combinational read may result in a combinational generation of the
  * ready/valid interface, which should be avoided.
  *
  * Read data valid or write buffer empty need to be polled ar status register.
  * Part of the write address determines the TDM slot and therefore the destination core.
  * Read data is data plus time stamp, which determines the source core.
  *
  * Possible enhancements: we could add a ready signal to support blocking reads
  * and writes depending on FIFO handshake. Is this worth it? Can always be built on
  * top of states polling in software or in hardware.
  *
  * @param w Usually 32-bits for a 32-bit processor.
  */
class CpuPort(private val w: Int) extends Bundle {
  val addr = Input(UInt(8.W))
  val rdData = Output(UInt(w.W))
  val wrData = Input(UInt(w.W))
  val rd = Input(Bool())
  val wr = Input(Bool())
}

// This should be a generic for the FIFO
class Entry(private val w: Int) extends Bundle {
  val data = UInt(w.W)
  val time = UInt(8.W)
}



class CpuInterface[T <: Data](dim: Int, txFifo: Int, rxFifo: Int, dt: T, width: Int) extends Module {
  val io = IO(new Bundle {
    val cpuPort = new CpuPort(width)
    val local = new Channel(dt)
  })

  // TODO: too much repetition
  // Either provide the schedule as parameter
  // or simply read out the TDM counter from the router.
  // Why duplicating it? Does it matter?
  val len = Schedule.getSchedule(dim)._1.length

  val regCnt = RegInit(0.U(log2Up(len).W))
  regCnt := Mux(regCnt === (len - 1).U, 0.U, regCnt + 1.U)
  // TDM schedule starts one cycles later for read data delay of OneWayMemory
  // Maybe we can use that delay here as well for something good
  val regDelay = RegNext(regCnt, init = 0.U)


  val entryReg = Reg(new Entry(width))
  when(io.cpuPort.wr) {
    entryReg.data := io.cpuPort.wrData
    entryReg.time := io.cpuPort.addr
  }

  val inFifo = Module(new BubbleFifo(rxFifo, width))
  inFifo.io.enq.write := false.B
  inFifo.io.enq.din.data := io.cpuPort.wrData
  inFifo.io.enq.din.time := io.cpuPort.addr
  when (io.cpuPort.wr && !inFifo.io.enq.full) {
    inFifo.io.enq.write := true.B
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
  outFifo.io.enq.write := false.B
  outFifo.io.enq.din.data := io.local.in.data
  outFifo.io.enq.din.time := regDelay
  when (io.local.in.valid && !outFifo.io.enq.full) {
    outFifo.io.enq.write := true.B
  }

  io.cpuPort.rdData := outFifo.io.deq.dout.data
  outFifo.io.deq.read := false.B
  when (io.cpuPort.rd) {
    val addr = io.cpuPort.addr
    when (addr === 0.U)  {
      outFifo.io.deq.read := true.B
    } .elsewhen(addr === 1.U) {
      io.cpuPort.rdData := outFifo.io.deq.dout.time
    } .elsewhen(addr === 2.U) {
      io.cpuPort.rdData := Cat(0.U(31.W), !inFifo.io.enq.full)
    } .elsewhen(addr === 3.U) {
      io.cpuPort.rdData := Cat(0.U(31.W), !outFifo.io.deq.empty)
    }
  }
}
