package noc

import chisel3._
import chisel3.tester._
import org.scalatest._

class ButterflyTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Router"

  it should "do something on direct connected router" in {
    test(new Butterfly()) { c =>
      c.io.inPorts(3).port(0).data.poke(7.U)
      for (i <- 0 until 10) {
        c.clock.step(1)
        println(c.io.outPorts(0).port(3).data.peek.litValue())
      }
      c.io.outPorts(0).port(3).data.expect(7.U)
    }
  }
}