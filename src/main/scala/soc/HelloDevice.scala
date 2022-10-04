package soc

import chisel3._

/**
  * An absolute minimal IO device, that is always ready.
  * It contains a single register to write and read at address 0
  * and the read-only core ID at address 1
  *
  * @param coreId
  */
class HelloDevice(coreId: Int) extends CpuInterface(2) {

  val nr = coreId.U(32.W)

  val reg = RegInit(0.U(32.W))

  cp.rdData := Mux(readAdrReg === 1.U, nr, reg)

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
