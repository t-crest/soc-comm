package soc

import chisel3._

/**
  * A Wishbone wrapper for the our PipeCon interface.
  *
  */
class WishboneWrapper(addrWidth: Int) extends WishboneDevice(addrWidth) {

  // TODO: switch to PipeConIO at some point
  val cpuIf = IO(new Bundle {
    val cpuPort = Flipped(new MemoryMappedIO(addrWidth-2))
  })
  val cp = cpuIf.cpuPort
  val wb = io.port

  cp.address := wb.addr >> 2
  cp.wrData := wb.wrData
  wb.rdData := cp.rdData
  wb.ack := cp.ack
  val cmd = wb.cyc & wb.stb
  cp.wr := wb.we & cmd
  cp.rd := !wb.we & cmd
}



