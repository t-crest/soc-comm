package wishbone

import chisel3._
import soc.HelloDevice

/**
  * Use our PipeCon Hello device wrapped into a Wishbone interface.
  *
  */
class WrappedHello() extends WishboneDevice(4) {

  val wrapper = Module(new Wrapper(4))
  val hello = Module(new HelloDevice(42))
  wrapper.cpuIf.cpuPort <> hello.cpuPort
  io.port <> wrapper.io.port
}



