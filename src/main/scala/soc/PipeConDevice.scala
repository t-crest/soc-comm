package soc

import chisel3._

/**
  * Just a CPU interface, without any additional connection.
  *
  */
abstract class PipeConDevice(addrWidth: Int) extends Module {

  val cpuPort = IO(new PipeCon(addrWidth))

  assert(addrWidth >= 2, "Address width needs some size for byte addresses")
}



