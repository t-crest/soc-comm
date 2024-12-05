package soc

import chisel3._

/**
  * Just a CPU interface, without any additional connection.
  *
  */
abstract class PipeCon(addrWidth: Int) extends Module {

  val cpuPort = IO(new PipeConIO(addrWidth))

  assert(addrWidth >= 2, "Address width needs some size for byte addresses")
}



