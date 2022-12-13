package wishbone

import chisel3._

/**
  * Just a Wishbone interface, without any additional connection.
  *
  */
abstract class WishboneDevice(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val port = Flipped(new WishboneIO(addrWidth))
  })
}



