package soc

import chisel3._
import chisel3.util._
import s4noc.Entry

/**
  * CPU interface to two ready/valid channels.
  *
  *
  * TODO: maybe networkPort should then have a different name?
  * TODO: this uses the S4NOC entry, this is not a generic CPU interface!
  * TODO: this is not complete, e.g. read is missing, not tested. Finish first the generic one.
  */
class CpuInterfaceS4NOC extends Module {
  val io = IO(new Bundle {
    val cpuPort = new MemoryMappedIO(4)
    val networkPort = new ReadyValidChannelsIO(Entry(UInt(32.W)))
  })

  val cp = io.cpuPort
  val tx = io.networkPort.tx
  val rx = io.networkPort.rx

  val idle :: read :: writeWait :: Nil = Enum(3)
  val stateReg = RegInit(idle)

  val rdyReg = RegInit(false.B)
  // TODO: how do I get the type of the entry?
  val dataReg = Reg(UInt(32.W))
  val addrReg = Reg(UInt(8.W))

  cp.ack := rdyReg

  // some default values to make it compile
  cp.rdData := 0.U
  rx.ready := false.B

  // write
  tx.valid := false.B
  tx.bits.data := Mux(stateReg === idle, cp.wrData, dataReg)
  tx.bits.time := Mux(stateReg === idle, cp.address, addrReg)
  tx.valid := cp.wr || stateReg === writeWait


  rdyReg := false.B
  switch(stateReg) {
    is(idle) {
      when(cp.wr) {
        when (tx.ready) {
          rdyReg := true.B
        } .otherwise {
          dataReg := cp.wrData
          addrReg := cp.address
          stateReg := writeWait
        }
      }
    }
    is (read) {

    }
    is (writeWait) {
      when (tx.ready) {
        rdyReg := true.B
        stateReg := idle
      }
    }
  }


  /*
  This is the combinational version
  when (io.cpuPort.wr && io.networkPort.tx.ready) {
    io.networkPort.tx.valid := true.B
  }

   */

}


object CpuInterfaceS4NOC extends App {
  emitVerilog(new CpuInterfaceS4NOC(), Array("--target-dir", "generated"))
}
