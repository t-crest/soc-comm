package wishbone

import chisel3._
import soc.PipeCon

/**
  * A Wishbone wrapper for the our PipeCon interface.
  *
  */
class Wrapper(addrWidth: Int) extends WishboneDevice(addrWidth) {

  // TODO: rename to PipeConIO at some point
  val cpuIf = IO(new Bundle {
    val cpuPort = Flipped(new PipeCon(addrWidth))
  })
  val cp = cpuIf.cpuPort
  val wb = io.port

  cp.address := wb.addr
  cp.wrData := wb.wrData
  cp.wrMask := "b1111".U
  wb.rdData := cp.rdData
  wb.ack := cp.ack
  val cmd = wb.cyc & wb.stb
  cp.wr := wb.we & cmd
  cp.rd := !wb.we & cmd
}



