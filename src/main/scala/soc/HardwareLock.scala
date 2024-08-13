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
class HardwareLock() extends PipeCon(0) {

  val lockReg = RegInit(false.B)
  val ackReg = RegInit(false.B)
  val readReg = RegInit(0.U)
  ackReg := cp.wr || cp.rd
  cp.ack := ackReg

  when (cp.rd) {
    readReg := 0.U
    when (!lockReg) {
      lockReg := true.B
      readReg := 1.U
    }
  }
  when (cp.wr) {
    lockReg := false.B
  }
  cp.rdData := readReg
}
