package slimflit

import chisel3._

class Packet extends Bundle {
  val x = SInt(4.W)
  val y = SInt(4.W)
  val src = UInt(8.W) // is this optional?
  val data = UInt(32.W)
}
