package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

import NocTester._

class NocTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Simple NoC Tester (just one packet)"

  "S4NoC" should "receive one packet old style, verbose" in {
    test(new S4NoCIO(4, 2, 2, 32)) { d =>

      def read3(): UInt = {
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
        val ret = read3()
        // the following looks like the way to do and compiles, but does not make sense
        // It is always false
        // if (!done) done = ret == "hcafebabe".U
        if (!done) done = ret.litValue() == "hcafebabe".U.litValue()
      }
      assert(done)
    }
  }

  it should "receive one packet, better coding, threaded" in {
    test(new S4NoCIO(4, 2, 2, 32)) { d =>
      // Master thread (core 0)
      // Write into time slot 0 to reach core 3
      fork {
        write(d.io.cpuPorts(0), 0.U, "hcafebabe".U, d.clock)
        // waste some time to see the concurrency
        for (i <- 0 until 20) {
          d.clock.step(1)
          println("Master "+i + " " + d.io.cycCnt.peek.litValue)
        }
      }

      // this is the reader thread (core 3)
      var done = false
      for (i <- 0 until 14) {
        val ret = read(d.io.cpuPorts(3), d.clock)
        if (!done) done = ret._2 == 0xcafebabe
        println("thread read "+i+" "+ret)
      }
      assert(done)
    }
  }
}

object NocTester {

  def isDataAvail(port: CpuPort, clock: Clock): Boolean = {
    port.rd.poke(true.B)
    port.addr.poke(3.U)
    val ret = port.rdData.peek.litValue == 1
    clock.step()
    port.rd.poke(false.B)
    ret
  }

  def isBufferFree(port: CpuPort, clock: Clock): Boolean = {
    port.rd.poke(true.B)
    port.addr.poke(2.U)
    val ret = port.rdData.peek.litValue == 1
    clock.step()
    port.rd.poke(false.B)
    ret
  }
  /**
    * Nonblocking read. Advances clock by one.
    * Cheats on reading multiple values in a single clock cycle.
    */
  def read(port: CpuPort, clock: Clock): (Boolean, Int, Int) = {

    val dataAvail = isDataAvail(port, clock)
    var data = 0
    var from = 0
    // FIXME: why is the return value earlier visible than status?
    // How many clock cycles should it be?
    if (dataAvail) {
      port.rd.poke(true.B)
      port.addr.poke(1.U)
      // Read before data to not trigger the FIFO read, but we read both in the same clock cycle
      // TODO: this is cheating! We cannot read out two different words in a single clock cycle.
      // TODO: we even read 3 values in a single clock cycle: status, data, and from
      // TODO: maybe merge from with data available
      // TODO: or have a "raw" interface to all signals, independent of a processor interface
      // TODO: the argument is to test the NI/NoC itself not the bus interface
      from = port.rdData.peek.litValue.toInt
      port.addr.poke(0.U)
      data = port.rdData.peek.litValue.toInt
    }
    // We need a clock step to have the rd signal one for one clock cycle
    clock.step(1)
    port.rd.poke(false.B)
    (dataAvail, data, from)
  }

  /**
    * Nonblocking write. Advances clock by two (checking buffer free).
    */
  def write(port: CpuPort, addr: UInt, data: UInt, clock:Clock): Boolean = {

    val bufferFree = isBufferFree(port, clock)
    if (bufferFree) {
      port.wrData.poke(data)
      port.addr.poke(addr)
      port.wr.poke(true.B)
    }
    clock.step(1)
    port.wrData.poke(0.U)
    port.addr.poke(0.U)
    port.wr.poke(false.B)
    bufferFree
  }

  def blockingWrite(port: CpuPort, addr: UInt, data: UInt, clock:Clock) = {
    while (!write(port, addr, data, clock)) {}
  }
}