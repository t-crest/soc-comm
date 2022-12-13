package s4noc

import chisel3._
import soc._


/**
  * Top level of the S4NOC with Wishbone interfaces.
  * Yet another layer on the original top level.
  *
  * Author: Martin Schoeberl (martin@jopdesign.com)
  * license see LICENSE
  * @param conf
  */

class S4NoCTopWB(conf: Config) extends Module  {
  val io = IO(new Bundle {
    val wbPorts = Vec(conf.n, new WishboneIO(4))
  })

  val s4noc = new S4NoCTop(conf)
  for (i <- 0 until conf.n) {
    val wb = Module(new WishboneWrapper(4))
    wb.cpuIf.cpuPort <> s4noc.io.cpuPorts(i)
    io.wbPorts(i) <> wb.io.port
  }
}

