package soc

import chisel3._
import chisel3.util._
import s4noc.Entry

/**
  * CPU interface to two ready/valid channels.
  * IO mapping as in classic PC serial port
  * 0: status (control): bit 0 transmit ready, bit 1 rx data available
  * 1: txd and rxd
  * 2: write receiver, read sender
  * TODO: compare with Chisel book version
  * TODO: make it generic and do a subtype for s4noc
  */
class PipeConRV[T <: Data](private val addrWidth: Int, private val dt: T, s4noc: Boolean = false ) extends PipeCon(addrWidth) {

  val rv = IO(new ReadyValidChannelsIO(dt))

  val tx = rv.tx
  val rx = rv.rx

  val ackReg = RegInit(false.B)
  cp.ack := ackReg

  val status = rx.valid ## tx.ready

  // Two additional registers, used by S4NOC for addressing of nodes
  val txDestReg = RegInit(0.U(8.W))
  val rxSourceReg = RegInit(0.U(8.W))
  // TODO: detect Entry type
  // println("Type: " + dt.isInstanceOf[Entry[UInt(32.W)]])

  // Some defaults
  rx.ready := false.B
  tx.valid := false.B
  if (s4noc) {
    val e = Wire(new Entry(UInt(32.W)))
    e.data := cp.wrData
    e.core := txDestReg
    rv.tx.bits := e
    cp.rdData := rv.rx.bits.asTypeOf(Entry(UInt(32.W))).data
  } else {
    rv.tx.bits := cp.wrData
    cp.rdData := rv.rx.bits
  }

  val idle :: readStatus :: readSource :: readWait :: writeWait :: Nil = Enum(5)
  val stateReg = RegInit(idle)
  val wrDataReg = Reg(UInt(32.W))

  def idleReaction() = {
    when (cp.wr) {
      // printf("Write %d to %d\n", cp.wrData, cp.address)
      when (cp.address === 2.U) {
        txDestReg := cp.wrData
        ackReg := true.B
      } .otherwise {
        tx.valid := true.B
        when (tx.ready) {
          ackReg := true.B
        } .otherwise {
          wrDataReg := cp.wrData
          stateReg := writeWait
        }
      }
    }
    when (cp.rd) {
      // printf("Read from %d\n", cp.address)
      when (cp.address === 0.U) {
        stateReg := readStatus
        ackReg := true.B
      } .elsewhen (cp.address === 2.U) {
        stateReg := readSource
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
    is (readSource) {
      cp.rdData := rxSourceReg
      stateReg := idle
      idleReaction()
    }
    is (readWait) {
      rx.ready := true.B
      when (rx.valid) {
        stateReg := idle
        if (s4noc) {
          rxSourceReg := rx.bits.asTypeOf(Entry(UInt(32.W))).core
        }
        // this is different from write - check
        cp.ack := true.B
        idleReaction()
      }
    }

    is (writeWait) {
      tx.valid := true.B
      if (s4noc) {
        val e = tx.bits.asTypeOf(Entry(UInt(32.W)))
        e.data := wrDataReg
      } else {
        tx.bits := wrDataReg
      }
      when (tx.ready) {
        stateReg := idle
        ackReg := true.B
        idleReaction()
      }
    }
  }
}



