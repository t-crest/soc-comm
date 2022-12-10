package soc

import chisel3._

/**
  * An minimal Wishbone IO device.
  * It contains a single register to write and read at address 0
  * and the read-only core ID at address 1.
  *
  * @param coreId
  */
class HelloWishbone() extends WishboneDevice(2) {

  io.port.rdData := 0.U
  io.port.ack := false.B
  /*
  // TODO: following five lines are duplicated in CpuInterfaceRV, back to CpuInterface?
  val addrReg = RegInit(0.U(2.W))
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
  cp.rdData := Mux(addrReg === 1.U, nr, reg)

   */
}
