
package soc

import chisel.lib.fifo._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import s4noc.Entry

class WishboneTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The Wishbone example device"

  it should "do something" in {
    test(new HelloWishbone()) {
      d => {
        def step() = d.clock.step()

        val p = d.io.port
        p.cyc.poke(true.B)
        p.stb.poke(true.B)
        p.we.poke(true.B)
        p.wrData.poke(1234.U)
        // How does WB decide between asynchronous devices and pipelined mode?
        // Probably a master would need to support both.
        // MS does NOT like the asynchronous mode
        p.ack.expect(false, "We do not want to use asynchronous slaves")
        step()
        // WB spec is inconsistent if stb stays high or not for more than one clock cycle (Figure 3.3 and 3.4)
        while(!p.ack.peekBoolean()) {
          // TODO: we should have a timeout
          step()
        }
        // now do the read, keep cyc and stb asserted
        p.we.poke(false.B)
        step()
        while (!p.ack.peekBoolean()) {
          // TODO: we should have a timeout
          step()
        }
        val rd = p.rdData.peekInt()
        assert(rd == 1234)
        step()
        p.ack.expect(false.B)
      }
    }
  }
}