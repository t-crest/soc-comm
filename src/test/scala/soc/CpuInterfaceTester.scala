
package soc

import chisel3._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._

class CpuInterfaceTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "The CpuInterface"

  it should "handle write timing" in {
    test(new CpuInterface).withAnnotations(Seq(chiseltest.internal.WriteVcdAnnotation)) {
      d => {

        def step() = d.clock.step()

        val cpu = d.io.cpuPort
        val noc = d.io.networkPort
        step()
        cpu.rdy.expect(false.B)

        // maybe set some default values

        // single cycle ready
        cpu.address.poke(3.U)
        cpu.wrData.poke(0x0123.U)
        cpu.wr.poke(true.B)
        noc.tx.ready.poke(true.B)
        step()
        noc.tx.bits.data.expect(0x0123.U)
        noc.tx.bits.time.expect(3.U)
        cpu.rdy.expect(true.B)
        // delayed ready
        cpu.wr.poke(false.B)
        cpu.address.poke(0.U)
        cpu.wrData.poke(0x0.U)
        step()
        noc.tx.ready.poke(false.B)
        cpu.address.poke(1.U)
        cpu.wrData.poke(0x456.U)
        cpu.wr.poke(true.B)
        step()
        cpu.wr.poke(false.B)
        cpu.address.poke(0.U)
        cpu.wrData.poke(0x0.U)
        cpu.rdy.expect(false.B)
        step()
        cpu.rdy.expect(false.B)
        step()
        noc.tx.ready.poke(true.B)
        noc.tx.bits.data.expect(0x0456.U)
        noc.tx.bits.time.expect(1.U)
        step()
        cpu.rdy.expect(true.B)
        step()
      }
    }
  }

  it should "work on a FIFO queue" in {
    test(new DirectLink) {
      d => {
        val a = d.io.a
        val b = d.io.b

        d.clock.step()
        // baby step test to test the test
        a.rdy.expect(false.B)
      }
    }
  }
}