package wishbone

import chisel3._
import soc.HardwareLock

/**
  * Use our PipeCon HardwareLock device wrapped into a Wishbone interface.
  *
  */
class WBHardwareLock() extends WishboneDevice(2) {

  val wrapper = Module(new Wrapper(2))
  val hello = Module(new HardwareLock())
  wrapper.cpuIf.cpuPort <> hello.cpuPort
  io.port <> wrapper.io.port
}



