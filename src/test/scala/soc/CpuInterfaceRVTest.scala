
package soc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CpuInterfaceRVTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The CpuInterfaceRV"

  it should "do something" in {
    test(new CpuInterfaceRV(4, UInt(32.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      d => {

        def step() = d.clock.step()

        val cp = d.io.cpuPort
        val rx = d.rv.rx
        val tx = d.rv.tx

        step()
        cp.ack.expect(false.B)

        // one cycle write
        cp.address.poke(0.U)
        cp.wrData.poke(0x0123.U)
        cp.wr.poke(true.B)
        tx.ready.poke(true.B)
        tx.bits.expect(0x0123.U)
        step()
        cp.wr.poke(false.B)
        cp.ack.expect(true.B)

        tx.ready.poke(false.B)
        rx.valid.poke(false.B)
        assert(d.read(0) == 0)
        tx.ready.poke(true.B)
        assert(d.read(0) == 1)
        tx.ready.poke(false.B)
        rx.valid.poke(true.B)
        assert(d.read(0) == 2)
        tx.ready.poke(true.B)
        assert(d.read(0) == 3)

        d.write(0, 0xcafe)
        tx.bits.expect(0xcafe)

        rx.bits.poke(123)
        assert(d.read(1) == 123)
      }
    }
  }
}