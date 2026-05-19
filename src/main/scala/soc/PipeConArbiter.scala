package soc

import chisel3._
import chisel3.util._

/**
  * Arbitration for several PipeCon masters sharing one PipeCon slave.
  *
  * Each input has a one-entry request register. This lets the arbiter capture
  * same-cycle request pulses from several masters, then replay them one at a
  * time on memPort. Responses are routed back only to the granted requester.
  */
class PipeConArbiter(addrWidth: Int, nrPorts: Int) extends Module {
  require(addrWidth >= 2, "PipeCon addresses are byte addresses")
  require(nrPorts >= 1, "PipeConArbiter needs at least one input port")

  val cpuPorts = IO(Vec(nrPorts, new PipeCon(addrWidth)))
  val memPort = IO(Flipped(new PipeCon(addrWidth)))

  val portBits = log2Ceil(nrPorts max 2)

  val pending = RegInit(VecInit(Seq.fill(nrPorts)(false.B)))
  val reqAddress = Reg(Vec(nrPorts, UInt(addrWidth.W)))
  val reqRead = Reg(Vec(nrPorts, Bool()))
  val reqWrite = Reg(Vec(nrPorts, Bool()))
  val reqWriteData = Reg(Vec(nrPorts, UInt(32.W)))
  val reqWriteMask = Reg(Vec(nrPorts, UInt(4.W)))

  val nextPort = RegInit(0.U(portBits.W))
  val activePort = RegInit(0.U(portBits.W))
  val responseData = RegInit(0.U(32.W))

  val sIdle :: sIssue :: sWait :: sAck :: Nil = Enum(4)
  val state = RegInit(sIdle)

  for (i <- 0 until nrPorts) {
    when(!pending(i) && (cpuPorts(i).rd || cpuPorts(i).wr)) {
      pending(i) := true.B
      reqAddress(i) := cpuPorts(i).address
      reqRead(i) := cpuPorts(i).rd
      reqWrite(i) := cpuPorts(i).wr
      reqWriteData(i) := cpuPorts(i).wrData
      reqWriteMask(i) := cpuPorts(i).wrMask
    }
  }

  val grantOh = Wire(Vec(nrPorts, Bool()))
  grantOh := VecInit(Seq.fill(nrPorts)(false.B))

  for (start <- 0 until nrPorts) {
    when(nextPort === start.U) {
      val order = (0 until nrPorts).map(offset => (start + offset) % nrPorts)
      for ((idx, pos) <- order.zipWithIndex) {
        val earlier = if (pos == 0) {
          false.B
        } else {
          VecInit(order.take(pos).map(i => pending(i))).asUInt.orR
        }
        grantOh(idx) := pending(idx) && !earlier
      }
    }
  }

  val grantValid = grantOh.asUInt.orR
  val grantPort = OHToUInt(grantOh.asUInt)
  val activeLast = activePort === (nrPorts - 1).U

  for (i <- 0 until nrPorts) {
    cpuPorts(i).ack := state === sAck && activePort === i.U
    cpuPorts(i).rdData := responseData
  }

  memPort.address := 0.U
  memPort.rd := false.B
  memPort.wr := false.B
  memPort.wrData := 0.U
  memPort.wrMask := 0.U

  switch(state) {
    is(sIdle) {
      when(grantValid) {
        activePort := grantPort
        state := sIssue
      }
    }

    is(sIssue) {
      memPort.address := reqAddress(activePort)
      memPort.rd := reqRead(activePort)
      memPort.wr := reqWrite(activePort)
      memPort.wrData := reqWriteData(activePort)
      memPort.wrMask := reqWriteMask(activePort)
      state := sWait
    }

    is(sWait) {
      when(memPort.ack) {
        responseData := memPort.rdData
        pending(activePort) := false.B
        nextPort := Mux(activeLast, 0.U, activePort + 1.U)
        state := sAck
      }
    }

    is(sAck) {
      state := sIdle
    }
  }
}
