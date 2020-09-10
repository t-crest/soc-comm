package s4noc

import chisel3._
import chisel3.util._

import soc._

class CpuInterfacte extends Module {
  val io = IO(new Bundle {
    val cpuPort = new IOPort(4)
    val networkPort = Flipped(new NetworkPort(UInt(32.W)))
  })
}
