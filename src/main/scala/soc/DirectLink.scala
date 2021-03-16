package soc

import chisel3._

class DirectLink extends Module {
  val io = IO(new Bundle{
    val a = new IOPort(4)
    val b = new IOPort(4)
  })

  val ifa = Module(new CpuInterface)
  val ifb = Module(new CpuInterface)

  ifa.io.cpuPort <> io.a
  ifb.io.cpuPort <> io.b

  ifa.io.networkPort.tx <> ifb.io.networkPort.rx
  ifa.io.networkPort.rx <> ifb.io.networkPort.tx

}
