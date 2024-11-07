package noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ButterflyTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Router"

  it should "do something on direct connected router" in {
    test(new Butterfly()) { c =>
      c.io.inPorts(3).port(0).data.poke(7.U)
      for (i <- 0 until 10) {
        c.clock.step(1)
        println(c.io.outPorts(0).port(3).data.peekInt())
      }
      c.io.outPorts(0).port(3).data.expect(7.U)
    }
  }
}