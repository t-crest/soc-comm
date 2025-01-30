package soc

import chisel3._

/**
  * A hardware lock.
  *
  * Use a read to try to get a lock.
  * Returns 1 on success and 0 if the lock is already taken.
  * Release the lock with a write.
  *
  * Address bits are currently not used. Could be extended to allow nested lock requests.
  *
  * Could be extended to a multicore lock.
  */
class HardwareLock() extends PipeConDevice(2) {

  val lockReg = RegInit(false.B)
  val ackReg = RegInit(false.B)
  val readReg = RegInit(0.U)
  ackReg := cpuPort.wr || cpuPort.rd
  cpuPort.ack := ackReg

  when (cpuPort.rd) {
    readReg := 0.U
    when (!lockReg) {
      lockReg := true.B
      readReg := 1.U
    }
  }
  when (cpuPort.wr) {
    lockReg := false.B
  }
  cpuPort.rdData := readReg
}
