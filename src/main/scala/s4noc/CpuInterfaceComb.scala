/*
  Processor interface for the S4NOC.

  Author: Martin Schoeberl (martin@jopdesign.com)
  license see LICENSE
 */
package s4noc

import chisel3._
import chisel3.util._
import soc.ReadyValidChannel

/**
  * A simple processor interface. No handshake.
  * Read timing is currently same clock cycle (combinational read).
  * In Patmos the usage of the read data is delayed by one clock cycle in the OCP wrapper.
  *
  * NOTE: this combinational read may result in a combinational generation of the
  * ready/valid interface, which should be avoided.
  *
  * Read data valid or write buffer empty need to be polled as a status register.
  * Part of the write address determines the TDM slot and therefore the destination core.
  * Read data is data plus time stamp, which determines the source core.
  *
  * Possible enhancements: we could add a ready signal to support blocking reads
  * and writes depending on FIFO handshake. Is this worth it? Can always be built on
  * top of states polling in software or in hardware.
  *
  * @param w Usually 32-bits for a 32-bit processor.
  */

class CpuPortComb(private val w: Int) extends Bundle {
  val addr = Input(UInt(8.W))
  val rdData = Output(UInt(w.W))
  val wrData = Input(UInt(w.W))
  val rd = Input(Bool())
  val wr = Input(Bool())
}

class CpuInterfaceComb[T <: Data](dt: T, width: Int) extends Module {
  val io = IO(new Bundle {
    val cpuPort = new CpuPortComb(width)
    val networkPort = new ReadyValidChannel(Entry(dt))
  })

  io.networkPort.tx.valid := false.B
  io.networkPort.tx.bits.data := io.cpuPort.wrData
  io.networkPort.tx.bits.time := io.cpuPort.addr
  when (io.cpuPort.wr && io.networkPort.tx.ready) {
    io.networkPort.tx.valid := true.B
  }

  // TODO: rd timing is the same clock cycle, do we want this?
  // Maybe we should do the AXI interface for all future designs
  // In our OCP interface we have defined it...

  // for now same clock cycle

  io.cpuPort.rdData := io.networkPort.rx.bits.data

  io.networkPort.rx.ready := false.B
  when (io.cpuPort.rd) {
    val addr = io.cpuPort.addr
    when (addr === 0.U)  {
      io.networkPort.rx.ready := true.B
    } .elsewhen(addr === 1.U) {
      io.cpuPort.rdData := io.networkPort.rx.bits.time
    } .elsewhen(addr === 2.U) {
      io.cpuPort.rdData := Cat(0.U(31.W), io.networkPort.tx.ready)
    } .elsewhen(addr === 3.U) {
      io.cpuPort.rdData := Cat(0.U(31.W), io.networkPort.rx.valid)
    }
  }
}
