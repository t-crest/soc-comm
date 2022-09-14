package soc

import chisel3._
import chisel3.util._

/**
  * Just a CPU interface, without any additional connection.
  *
  *
  */
class CpuInterface(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpuPort = new CpuPortIO(addrWidth)
  })

  val cp = io.cpuPort

  val idle :: waitRead :: waitWrite :: Nil = Enum(3)
  val stateReg = RegInit(idle)

  val rdyReg = RegInit(false.B)
  val dataReg = Reg(UInt(32.W))

  // TODO: do we need to store the read address in a register?

  cp.ack := rdyReg

  // some default values
  cp.rdData := 0.U


  val readyToWrite = WireDefault(true.B)
  val readyToRead = WireDefault(true.B)

  rdyReg := false.B
  switch(stateReg) {
    is(idle) {
      when(cp.wr) {
        when (readyToWrite) {
          rdyReg := true.B
        } .otherwise {
          dataReg := cp.wrData
          stateReg := waitWrite
        }
      }
      when (cp.rd) {
        when (readyToRead) {
          rdyReg := true.B
        } .otherwise {
          stateReg := waitRead
        }
      }
    }
    is (waitRead) {

    }
    is (waitWrite) {
      when (readyToWrite) {
        rdyReg := true.B
        stateReg := idle
      }
    }
  }
}



