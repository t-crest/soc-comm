package soc

import chisel3._

/**
  * An minimal IO device.
  * It contains a single register to write and read at address 0
  * and the read-only core ID at address 4.
  *
  * @param coreId
  */
class HelloDevice(coreId: Int) extends PipeCon(3) {

  // TODO: following five lines are duplicated in PipeConRV, back to PipeCon class?
  val addrReg = RegInit(0.U(3.W))
  val ackReg = RegInit(false.B)
  when (cp.rd) {
    addrReg := cp.address
  }
  cp.ack := ackReg

  val nr = coreId.U(32.W)
  val reg = RegInit(0.U(32.W))
  when (cp.wr) {
    reg := cp.wrData
  }
  ackReg := cp.wr || cp.rd
  cp.rdData := Mux(addrReg === 4.U, nr, reg)
}

class MultiCoreHello(nrCores: Int) extends MultiCoreDevice(nrCores, 3) {

  for(i <- 0 until nrCores) {
    val d = Module(new HelloDevice(i))
    d.io.cpuPort <> ports(i)
  }
}
