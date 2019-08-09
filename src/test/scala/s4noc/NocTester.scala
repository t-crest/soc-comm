package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

class NocTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Simple NoC Tester"

  it should "test one packet" in {
    test(new S4noc(4, 2, 2, 32)) { d =>

      def read(): UInt = {
        d.io.cpuPorts(3).rd.poke(true.B)
        d.io.cpuPorts(3).addr.poke(3.U)
        // I find it a bit confusing to deal with Chisel types at runtime in Scala
        // e.g., forget to use litValue using the Scala compare resulted in always false
        // litValue is no an intuitive function to "get a Scala usable value"
        val status = d.io.cpuPorts(3).rdData.peek().litValue()
        println("status="+status)
        var ret = 0.U
        d.clock.step(1)
        // FIXME: why is the return value earlier visible than status?
        // How many clock cycles should it be?
        if (status == 1) {
          d.io.cpuPorts(3).rd.poke(true.B)
          d.io.cpuPorts(3).addr.poke(0.U)
          ret = d.io.cpuPorts(3).rdData.peek()
          println("ret="+ret.toString())
          d.clock.step(1)
          d.io.cpuPorts(3).rd.poke(true.B)
          d.io.cpuPorts(3).addr.poke(1.U)
          val from = d.io.cpuPorts(3).rdData.peek()
          d.clock.step(1)
          // d.io.cpuPorts(3).rd.poke(false.B)
        }
        ret
      }
      d.io.cpuPorts(0).wrData.poke("hcafebabe".U)
      d.io.cpuPorts(0).addr.poke(0.U)
      d.io.cpuPorts(0).wr.poke(true.B)
      d.clock.step(1)
      d.io.cpuPorts(0).wrData.poke(0.U)
      d.io.cpuPorts(0).addr.poke(0.U)
      d.io.cpuPorts(0).wr.poke(false.B)

      var done = false
      for (i <- 0 until 14) {
        val ret = read()
        // the following looks like the way to do and compiles, but does not make sense
        // if (!done) done = ret == "hcafebabe".U
        if (!done) done = ret.litValue() == "hcafebabe".U.litValue()
      }
      assert(done)
    }

  }

}