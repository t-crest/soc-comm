
package soc

import chisel3._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest._

class HelloTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "The HelloDevice"

  it should "have single cycle timing" in {
    test(new HelloDevice(3)).withAnnotations(Seq(chiseltest.internal.WriteVcdAnnotation)) {
      d => {

        def step() = d.clock.step()

        val cp = d.io.cpuPort
        cp.rdy.expect(false.B)

        // set some default values
        cp.address.poke(0.U)
        cp.wrData.poke(0.U)
        cp.wr.poke(false.B)
        cp.rd.poke(false.B)

        step()
        cp.rdy.expect(false.B)

        // single cycle write
        cp.address.poke(0.U)
        cp.wrData.poke(0x0123.U)
        cp.wr.poke(true.B)
        step()
        // Next clock cycle rdy
        cp.rdy.expect(true.B)

        // delayed ready
        cp.wr.poke(false.B)
        cp.address.poke(0.U)
        cp.wrData.poke(0x0.U)
        step()
        cp.rdy.expect(false.B)

        // Do a read, expect a single cycle response
        cp.rd.poke(true.B)
        cp.address.poke(0.U)
        step()
        cp.rdy.expect(true.B)
        cp.rdData.expect(0x0123.U)
        // Back to back read/write/read
        cp.rd.poke(false.B)
        cp.wr.poke(true.B)
        cp.wrData.poke(0xabcd.U)
        step()
        cp.rdy.expect(true.B)
        cp.rd.poke(true.B)
        cp.wr.poke(false.B)
        step()
        cp.rdy.expect(true.B)
        cp.rdData.expect(0xabcd.U)
        //test CPU ID
        cp.address.poke(0x1.U)
        step()
        cp.rdData.expect(3.U)
        cp.rdy.expect(true.B)
        // Back to register read
        cp.address.poke(0x0.U)
        step()
        cp.rdy.expect(true.B)
        cp.rdData.expect(0xabcd.U)
        cp.rd.poke(false.B)
        step()
        cp.rdy.expect(false.B)



      }
    }
  }

}