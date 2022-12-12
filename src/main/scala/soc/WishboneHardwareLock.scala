package soc

import chisel3._

/**
  * Use our PipeCon HardwareLock device wrapped into a Wishbone interface.
  *
  */
class WishboneHardwareLock() extends WishboneDevice(2) {

  val wrapper = Module(new WishboneWrapper(2))
  val hello = Module(new HardwareLock())
  wrapper.cpuIf.cpuPort <> hello.cp
  io.port <> wrapper.io.port
}



