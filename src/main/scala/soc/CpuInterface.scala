package soc

import chisel3._

/**
  * Just a CPU interface, without any additional connection.
  *
  */
abstract class CpuInterface(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpuPort = new MemoryMappedIO(addrWidth)
  })
  val cp = io.cpuPort
}



