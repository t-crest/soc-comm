package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

class NocTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Simple NoC Tester"

  it should "receive one packet old style, verbose" in {
    test(new S4noc(4, 2, 2, 32)) { d =>

      def read(): UInt = {
        d.io.cpuPorts(3).rd.poke(true.B)
        d.io.cpuPorts(3).addr.poke(3.U)
        // I find it a bit confusing to deal with Chisel types at runtime in Scala
        // e.g., forget to use litValue using the Scala compare resulted in always false
        // litValue is no an intuitive function to "get a Scala usable value"
        // Is there a way to compare Chisel type values for equality?
        val status = d.io.cpuPorts(3).rdData.peek().litValue()
        var ret = 0.U
        d.clock.step(1)
        // FIXME: why is the return value earlier visible than status?
        // How many clock cycles should it be?
        if (status == 1) {
          d.io.cpuPorts(3).rd.poke(true.B)
          d.io.cpuPorts(3).addr.poke(0.U)
          ret = d.io.cpuPorts(3).rdData.peek()
          d.clock.step(1)
          d.io.cpuPorts(3).rd.poke(true.B)
          d.io.cpuPorts(3).addr.poke(1.U)
          val from = d.io.cpuPorts(3).rdData.peek()
          d.clock.step(1)
          d.io.cpuPorts(3).rd.poke(false.B)
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
        // It is always false
        // if (!done) done = ret == "hcafebabe".U
        if (!done) done = ret.litValue() == "hcafebabe".U.litValue()
      }
      assert(done)
    }
  }

  it should "receive one packet, better coding, threaded" in {
    test(new S4noc(4, 2, 2, 32)) { d =>

      def read(port: CpuPort): Int = {
        port.rd.poke(true.B)
        port.addr.poke(3.U)
        val status = port.rdData.peek().litValue()
        var ret = 0
        d.clock.step(1)
        // FIXME: why is the return value earlier visible than status?
        // How many clock cycles should it be?
        if (status == 1) {
          port.rd.poke(true.B)
          port.addr.poke(0.U)
          ret = port.rdData.peek().litValue().toInt
          // We don't need to step to read the other value, do we?
          d.clock.step(1)
          port.rd.poke(true.B)
          port.addr.poke(1.U)
          // Is from still there? I assume it needs to be read before data to not trigger the FIFO read
          val from = port.rdData.peek()
          d.clock.step(1)
          // default is not reading
          port.rd.poke(false.B)
        }
        ret
      }

      def write(port: CpuPort, data: UInt) {
        port.wrData.poke(data)
        port.addr.poke(0.U)
        port.wr.poke(true.B)
        d.clock.step(1)
        port.wrData.poke(0.U)
        port.addr.poke(0.U)
        port.wr.poke(false.B)
      }

      // this is thre reader thread
      val th = fork {
        var done = false
        for (i <- 0 until 14) {
          val ret = read(d.io.cpuPorts(3))
          if (!done) done = ret == 0xcafebabe
          println("thread read "+i+" "+ret)
        }
        assert(done)
      }



      // Master thread
      write(d.io.cpuPorts(0), "hcafebabe".U)
      //waste some time to see the concurrency
      for (i <- 0 until 20) {
        d.clock.step(1)
        println("Master "+i)
      }

      th.join()

    }
  }
}