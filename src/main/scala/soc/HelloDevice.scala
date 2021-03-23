package soc

import chisel3._

/**
  * An absolute minimal IO device, that is always ready.
  * It contains a single register to write and read, and the read-only core ID
  *
  * @param coreId
  */
class HelloDevice(coreId: Int) extends CpuInterface(2) {

  val nr = coreId.U(32.W)

  val reg = RegInit(0.U(32.W))

  val readAdr = RegInit(0.U(1.W))

  when (cp.rd) {
    readAdr := cp.address
  }

  cp.rdData := Mux(readAdr === 1.U, nr, reg)

  when (cp.wr) {
    reg := cp.wrData
  }
}

class MultiCoreHello(nrCores: Int) extends MultiCoreDevice(nrCores, 2) {

  for(i <- 0 until nrCores) {
    val d = Module(new HelloDevice(i))
    d.io.cpuPort <> io.ports(i)
  }
}
