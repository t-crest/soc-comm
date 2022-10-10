package soc

import chisel3._
import chisel3.util._
import s4noc.Entry

/**
  * CPU interface to two ready/valid channels.
  * IO mapping as in classic PC serial port
  * 0: status (control): bit 0 transmit ready, bit 1 rx data available
  * 1: txd and rxd
  * TODO: use it somewhere, maybe with a RV serial port
  * TODO: compare with Chisel book version
  *
  */
class CpuInterfaceRV[T <: Data](private val addrWidth: Int, private val dt: T) extends CpuInterface(addrWidth) {

  val rv = IO(new ReadyValidChannelsIO(dt))

  val tx = rv.tx
  val rx = rv.rx

  val status = rx.valid ## tx.ready

  // Some defaults
  rx.ready := false.B
  tx.valid := false.B
  tx.bits := cp.wrData
  cp.rdData := rx.bits

  val idle :: readStatus :: readWait :: writeWait :: Nil = Enum(4)
  val stateReg = RegInit(idle)
  val wrDataReg = Reg(UInt(32.W))

  def idleReaction() = {
    when (cp.wr) {
      tx.valid := true.B
      when (tx.ready) {
        ackReg := true.B
      } .otherwise {
        wrDataReg := cp.wrData
        stateReg := writeWait
      }
    }
    when (cp.rd) {
      when (cp.address === 0.U) {
        stateReg := readStatus
        ackReg := true.B
      } .otherwise {
        stateReg := readWait
      }
    }
  }

  ackReg := false.B
  switch(stateReg) {
    is (idle) {
      idleReaction()
    }
    is (readStatus) {
      cp.rdData := status
      stateReg := idle
      idleReaction()
    }
    is (readWait) {
      rx.ready := true.B
      when (rx.valid) {
        stateReg := idle
        cp.ack := true.B
        idleReaction()
      }
    }
    is (writeWait) {
      tx.valid := true.B
      tx.bits := wrDataReg
      when (tx.ready) {
        stateReg := idle
        cp.ack := true.B
        idleReaction()
      }
    }
  }
}



