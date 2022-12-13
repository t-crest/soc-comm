
package wishbone

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class WishboneTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The Wishbone stuff"

  it should "work with the example device" in {
    test(new Hello()) {
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

  it should "work as a hello wrapper" in {
    test(new WrappedHello()) {
      d => {
        def step() = d.clock.step()

        val p = d.io.port
        p.cyc.poke(true.B)
        p.stb.poke(true.B)
        p.we.poke(true.B)
        p.addr.poke(0.U)
        p.wrData.poke(1234.U)
        step()
        p.cyc.poke(false.B)
        p.stb.poke(false.B)
        p.we.poke(false.B)
        p.ack.expect(true.B)
        step()
        p.ack.expect(false.B)
        p.cyc.poke(true.B)
        p.stb.poke(true.B)
        step()
        p.ack.expect(true.B)
        p.rdData.expect(1234.U)
        p.addr.poke(4)
        step()
        p.ack.expect(true.B)
        p.rdData.expect(42.U)

        val wbh = new WishboneIOHelper(d.io.port, d.clock)
        wbh.write(0, 0xcafe)
        assert(wbh.read(0) == 0xcafe)

      }
    }
  }

  it should "the lock should work" in {
    test(new WBHardwareLock()) {
      d => {
        val wbh = new WishboneIOHelper(d.io.port, d.clock)

        // grab the lock
        assert(wbh.read(0) == 1)
        // should not be possible to grab again
        assert(wbh.read(0) == 0)
        // release the lock
        wbh.write(0, 0)
        // should be possible to grab again
        assert(wbh.read(0) == 1)

      }
    }
  }
}