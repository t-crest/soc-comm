
package soc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HelloTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The HelloDevice"

  it should "have single cycle timing" in {
    test(new HelloDevice(3)) {
      d => {

        def step() = d.clock.step()

        val cp = d.cpuPort
        cp.ack.expect(false.B)

        // set some default values
        cp.address.poke(0.U)
        cp.wrData.poke(0.U)
        cp.wr.poke(false.B)
        cp.rd.poke(false.B)

        step()
        cp.ack.expect(false.B)

        // single cycle write
        cp.address.poke(0.U)
        cp.wrData.poke(0x0123.U)
        cp.wr.poke(true.B)
        step()
        // Next clock cycle rdy
        cp.ack.expect(true.B)

        // delayed ready
        cp.wr.poke(false.B)
        cp.address.poke(0.U)
        cp.wrData.poke(0x0.U)
        step()
        cp.ack.expect(false.B)

        // Do a read, expect a single cycle response
        cp.rd.poke(true.B)
        cp.address.poke(0.U)
        step()
        cp.ack.expect(true.B)
        cp.rdData.expect(0x0123.U)
        // Back to back read/write/read
        cp.rd.poke(false.B)
        cp.wr.poke(true.B)
        cp.wrData.poke(0xabcd.U)
        step()
        cp.ack.expect(true.B)
        cp.rd.poke(true.B)
        cp.wr.poke(false.B)
        step()
        cp.ack.expect(true.B)
        cp.rdData.expect(0xabcd.U)
        //test CPU ID
        cp.address.poke(0x4.U)
        step()
        cp.rdData.expect(3.U)
        cp.ack.expect(true.B)
        // Back to register read
        cp.address.poke(0x0.U)
        step()
        cp.ack.expect(true.B)
        cp.rdData.expect(0xabcd.U)
        cp.rd.poke(false.B)
        step()
        cp.ack.expect(false.B)


      }
    }
  }

  it should "work as multi-core" in {
    test(new MultiCoreHello(3)) {
      dut => {

        def step() = dut.clock.step()

        val cp = dut.ports

        def setDefault() = {
          for (i <- 0 until 3) {
            cp(i).address.poke(0.U)
            cp(i).wrData.poke(0.U)
            cp(i).wr.poke(false.B)
            cp(i).rd.poke(false.B)
          }
        }

        cp(0).ack.expect(false.B)
        // set some default values
        setDefault()

        step()
        cp(0).wr.poke(true.B)
        cp(0).wrData.poke(123.U)
        cp(1).wr.poke(true.B)
        cp(1).wrData.poke(456.U)
        cp(2).wr.poke(true.B)
        cp(2).wrData.poke(789.U)
        step()
        setDefault()
        cp(0).rd.poke(true.B)
        cp(1).rd.poke(true.B)
        cp(2).rd.poke(true.B)
        step()
        cp(0).rdData.expect(123.U)
        cp(1).rdData.expect(456.U)
        cp(2).rdData.expect(789.U)
        cp(0).address.poke(4.U)
        cp(1).address.poke(4.U)
        cp(2).address.poke(4.U)
        step()
        cp(0).rdData.expect(0.U)
        cp(1).rdData.expect(1.U)
        cp(2).rdData.expect(2.U)
      }
    }
  }

  it should "use functions to access" in {
    test(new HelloDevice(3)).withAnnotations(Seq(WriteVcdAnnotation)) {
      dut => {
        def step() = dut.clock.step()

        val helper = new MemoryMappedIOHelper(dut.cpuPort, dut.clock)

        helper.setTimeOut(10)
        helper.read(4)
        step()
        step()
        // println(helper.read(0))
        // println(helper.read(4))
        step()
        step()
        helper.write(0, 123)
        helper.write(0, 456)
        assert(helper.read(0) == 456)
        assert(helper.read(0) == 456)

      }
    }
  }
}