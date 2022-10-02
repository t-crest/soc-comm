package soc

import chisel3._

class DirectLink extends Module {
  val io = IO(new Bundle{
    val a = new MemoryMappedIO(4)
    val b = new MemoryMappedIO(4)
  })

  val ifa = Module(new CpuInterfaceS4NOC)
  val ifb = Module(new CpuInterfaceS4NOC)

  ifa.io.cpuPort <> io.a
  ifb.io.cpuPort <> io.b

  ifa.io.networkPort.tx <> ifb.io.networkPort.rx
  ifa.io.networkPort.rx <> ifb.io.networkPort.tx

}
