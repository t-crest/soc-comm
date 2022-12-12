package soc

import chisel3._

/**
  * Use our PipeCon Hello device wrapped into a Wishbone interface.
  *
  */
class WishboneWrappedHello() extends WishboneDevice(4) {

  val wrapper = Module(new WishboneWrapper(4))
  val hello = Module(new HelloDevice(42))
  wrapper.cpuIf.cpuPort <> hello.cp
  io.port <> wrapper.io.port
}



