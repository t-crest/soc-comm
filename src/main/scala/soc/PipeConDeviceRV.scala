package soc

import chisel3._
import chisel3.util._
import s4noc.Entry

/**
  * CPU interface to two ready/valid channels.
  * IO mapping as in classic PC serial port
  * 0: status (control): bit 0 transmit ready, bit 1 rx data available
  * 4: txd and rxd
 * S4NOC:
  * 8: write receiver, read sender
  * TODO: compare with Chisel book version
  * TODO: make it generic and do a subtype for s4noc
  * TODO: have some better tests
  */
class PipeConDeviceRV[T <: Data](private val addrWidth: Int, private val dt: T, s4noc: Boolean = false) extends PipeConDevice(addrWidth) {

  val rv = IO(new ReadyValidChannelsIO(dt))

  val tx = rv.tx
  val rx = rv.rx

  val ackReg = RegInit(false.B)
  ackReg := false.B
  cpuPort.ack := ackReg
  // just look at lower bits
  val address = cpuPort.address(3, 0)

  val status = rx.valid ## tx.ready

  // This is ugly: two different transmit registers for two different types
  // dt should be usable here...
  val txDataReg = RegInit(0.U.asTypeOf(dt))
  val e = Wire(Entry(UInt(32.W)))
  e.data := 0.U
  e.core := 0.U
  val txS4NocReg = RegInit(e)
  // Aditional register, used by S4NOC for addressing of nodes
  val rxSourceReg = RegInit(0.U(8.W))
  // TODO: detect Entry type
  // println("Type: " + dt.isInstanceOf[Entry[UInt(32.W)]])

  // Some defaults
  rx.ready := false.B
  tx.valid := false.B
  if (s4noc) {
    cpuPort.rdData := rx.bits.asTypeOf(Entry(UInt(32.W))).data
    tx.bits := txS4NocReg
  } else {
    cpuPort.rdData := rx.bits
    tx.bits := txDataReg
  }

  val idle :: readStatus :: readSource :: readWait :: writeWait :: Nil = Enum(5)
  val stateReg = RegInit(idle)

  def idleReaction() = {
    when (cpuPort.wr) {
      // printf("Write %d to %d\n", cp.wrData, address)
      when (address === 8.U) {
        txS4NocReg.core := cpuPort.wrData
        ackReg := true.B
      } .elsewhen (address === 4.U) {
        /* lets not have this single cycle write for now
        tx.valid := true.B
        when (tx.ready) {
          ackReg := true.B
        } .otherwise {
          txReg.data := cpuPort.wrData
          stateReg := writeWait
        }
         */
        if (s4noc) {
          txS4NocReg.data := cpuPort.wrData
        } else {
          txDataReg := cpuPort.wrData
        }
        stateReg := writeWait
      }
    }
    when (cpuPort.rd) {
      // printf("Read from %d\n", address)
      when (address === 0.U) {
        stateReg := readStatus
        ackReg := true.B
      } .elsewhen (address === 8.U) {
        stateReg := readSource
        ackReg := true.B
      } .otherwise {
        stateReg := readWait
      }
    }
  }

  switch(stateReg) {
    is (idle) {
      idleReaction()
    }
    is (readStatus) {
      cpuPort.rdData := status
      stateReg := idle
      ackReg := false.B
      idleReaction()
    }
    is (readSource) {
      cpuPort.rdData := rxSourceReg
      stateReg := idle
      ackReg := false.B
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
        cpuPort.ack := true.B
        ackReg := false.B
        idleReaction()
      }
    }

    is (writeWait) {
      tx.valid := true.B
      when (tx.ready) {
        stateReg := idle
        ackReg := true.B
        idleReaction()
      }
    }
  }
}



