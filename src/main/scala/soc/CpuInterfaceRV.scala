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

  rx.ready := false.B
  tx.valid := false.B
  tx.bits := 0.U
  cp.rdData := 0.U

  // ack - this is now a non-blocking
  ackReg := cp.rd || cp.wr

  val rdDlyReg = RegInit(false.B)
  rdDlyReg := cp.rd // Needed?

  rx.ready := false.B
  when (addrReg === 1.U && rdDlyReg) {
    rx.ready := true.B
  }
  cp.rdData := Mux(addrReg === 0.U, status, rx.bits)

  // write to tx
  tx.bits := cp.wrData
  tx.valid := cp.wr

  /*
  // this is probably the blocking version
  val idle :: read :: writeWait :: Nil = Enum(3)
  val stateReg = RegInit(idle)
  val dataReg = Reg(UInt(32.W))


  // some default values
  cp.rdData := 0.U
  rx.ready := false.B

  // write
  tx.valid := false.B
  tx.bits := Mux(stateReg === idle, cp.wrData, dataReg)
  tx.valid := cp.wr || stateReg === writeWait


  ackReg := false.B
  switch(stateReg) {
    is(idle) {
      when(cp.wr) {
        when (tx.ready) {
          ackReg := true.B
        } .otherwise {
          dataReg := cp.wrData
          stateReg := writeWait
        }
      }
    }
    is (read) {

    }
    is (writeWait) {
      when (tx.ready) {
        ackReg := true.B
        stateReg := idle
      }
    }
  }

   */
}



