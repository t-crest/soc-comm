package soc

import chisel3._

/**
  * Just a CPU interface, without any additional connection.
  * Default implementation of single cycle read and write.
  * TODO: can this also be extended to do a multi-cycle read
  * and write, so we can still use the read and write test functions?
  *
  */
abstract class CpuInterface(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpuPort = new MemoryMappedIO(addrWidth)
  })

  val cp = io.cpuPort

  // register the (read) address
  val addrReg = RegInit(0.U(addrWidth.W))
  // register for the ack signal
  val ackReg = RegInit(false.B)

  when (cp.rd) {
    addrReg := cp.address
  }
  cp.ack := ackReg


  /*
  val idle :: waitRead :: waitWrite :: Nil = Enum(3)
  val stateReg = RegInit(idle)
  // TODO: what was the intention of this?
  val readyToWrite = WireDefault(true.B)
  val readyToRead = WireDefault(true.B)

  // Really a FSM here?
  // This looks a bit complicated. Maybe leave it up to the concrete
  // devices to do the handshake.
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
// TODO: fill in
    }
    is (waitWrite) {
      when (readyToWrite) {
        rdyReg := true.B
        stateReg := idle
      }
    }
  }

   */
}



