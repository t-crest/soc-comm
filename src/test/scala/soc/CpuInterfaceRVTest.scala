
package soc

import chisel.lib.fifo._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CpuInterfaceRVTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The CpuInterfaceRV"

  it should "do something" in {
    test(new CpuInterfaceRV(4, UInt(32.W))) {
      d => {

        def step() = d.clock.step()

        val cp = d.io.cpuPort
        val rx = d.rv.rx
        val tx = d.rv.tx

        val helper = new MemoryMappedIOHelper(d)

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

        println(helper.read(0))

        tx.ready.poke(false.B)
        rx.valid.poke(false.B)
        assert(helper.read(0) == 0)
        tx.ready.poke(true.B)
        assert(helper.read(0) == 1)
        tx.ready.poke(false.B)
        rx.valid.poke(true.B)
        assert(helper.read(0) == 2)
        tx.ready.poke(true.B)
        assert(helper.read(0) == 3)

        helper.write(0, 0xcafe)
        tx.bits.expect(0xcafe)

        rx.bits.poke(123)
        assert(helper.read(1) == 123)
      }
    }
  }

  it should "work with a FIFO connected between tx and rx" in {
    test(new Module() {
      val io = IO(new Bundle{
        val cpuPort = new MemoryMappedIO(4)
      })

      val cpif = Module(new CpuInterfaceRV(4, UInt(32.W)))
      val fifo = Module(new BubbleFifo(UInt(32.W), 4))

      io.cpuPort <> cpif.io.cpuPort
      cpif.tx <> fifo.io.enq
      cpif.rx <> fifo.io.deq
    }) { d =>
      // should get back at some time
      // sadly I cannot access that internal port and te function for it :-(
      // now needing to repeat code
      // d.cpif.write(1, 0xabcd)

      d.clock.step()
    }
  }



  it should "work with a FIFO connected between tx and rx..." in {
    test {
      class MyModule() extends CpuInterfaceRV(4, UInt(32.W)) {
        val cpif = Module(new CpuInterfaceRV(4, UInt(32.W)))
        val fifo = Module(new BubbleFifo(UInt(32.W), 4))
        // reconnect (overwrite) CPU interface
        cp <> cpif.io.cpuPort
        cpif.tx <> fifo.io.enq
        cpif.rx <> fifo.io.deq
      }
      new MyModule()
    }.withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      d.clock.step(4)
      val helper = new MemoryMappedIOHelper(d)

      // should get back at some time
      helper.write(1, 0x1234)
      var ok = false
      // TODO use ok in loop
      for (i <- 0 until 10 if !ok) {
        if ((helper.read(0) & 0x02) != 0) {
          assert(helper.read(1) == 0x1234)
          // println(d.read(0))
          ok = true
        }
      }
      // println(d.read(0))
      assert(ok)
      // assert((helper.read(0) & 0x02) == 0)
    }
  }
}