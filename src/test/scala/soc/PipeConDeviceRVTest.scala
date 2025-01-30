
package soc

import chisel.lib.fifo._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import s4noc.Entry

class PipeConDeviceRVTest() extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The CpuInterfaceRV"

  it should "do something" in {
    test(new PipeConDeviceRV(4, UInt(32.W))).withAnnotations(Seq(WriteVcdAnnotation)) {
      d => {

        def step() = d.clock.step()

        val cp = d.cpuPort
        val rx = d.rv.rx
        val tx = d.rv.tx

        val helper = new MemoryMappedIOHelper(d.cpuPort, d.clock)

        step()
        cp.ack.expect(false.B)

        // useful default
        cp.wrMask.poke(0xf.U)

        // two cycle write into tx register
        // TODO: should be a single cycle operation when the tx channel is ready
        cp.address.poke(4.U)
        cp.wrData.poke(0x0123.U)
        cp.wr.poke(true.B)
        tx.ready.poke(true.B)
        tx.valid.expect(false.B)
        step()
        tx.bits.expect(0x0123.U)
        tx.valid.expect(true.B)

        cp.wr.poke(false.B)
        step() // TODO: this should not be needed
        cp.ack.expect(true.B)
        step()
        cp.ack.expect(false.B)

        // one cycle read of status register
        cp.address.poke(0.U)
        cp.rd.poke(true.B)
        cp.ack.expect(false.B)
        step()
        cp.rd.poke(false.B)
        cp.ack.expect(true.B)
        cp.rdData.expect(0x01.U)
        step()
        cp.ack.expect(false.B)



        // println(helper.read(0))

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
        step()

        helper.write(4, 0xcafe)
        tx.bits.expect(0xcafe)

        rx.bits.poke(123)
        assert(helper.read(4) == 123)
      }
    }
  }

  // Connect a CPU interface to a FIFO
  class MyModule() extends PipeConDevice(4) {
    val cpif = Module(new PipeConDeviceRV(4, UInt(32.W)))
    val fifo = Module(new BubbleFifo(UInt(32.W), 4))
    cpuPort <> cpif.cpuPort
    cpif.tx <> fifo.io.enq
    cpif.rx <> fifo.io.deq
  }

  it should "work with a FIFO connected between tx and rx" in {
    test(new MyModule()) { d =>
      d.clock.step(2)
      val helper = new MemoryMappedIOHelper(d.cpuPort, d.clock)

      // should get back the data at some time
      helper.write(4, 0x1234)
      var ok = false
      for (i <- 0 until 10 if !ok) {
        if (helper.rxAvail) {
          assert(helper.read(4) == 0x1234)
          ok = true
        }
      }

      assert(ok)
      assert((helper.read(0) & 0x02) == 0)

      for (i <- 0 until 4) helper.sndWithCheck(i + 1)
      for (i <- 0 until 4) assert(helper.rcvWithCheck() == i + 1)
    }
  }

  // Connect two CPU interfaces to two FIFOs
  class MyModule2() extends Module {
    val io = IO(new Bundle {
      val cpA = new PipeCon(4)
      val cpB = new PipeCon(4)
    })
    val cpifA = Module(new PipeConDeviceRV(4, UInt(32.W)))
    val cpifB = Module(new PipeConDeviceRV(4, UInt(32.W)))
    val fifoA = Module(new MemFifo(UInt(32.W), 4))
    val fifoB = Module(new MemFifo(UInt(32.W), 4))

    io.cpA <> cpifA.cpuPort
    io.cpB <> cpifB.cpuPort
    cpifA.tx <> fifoA.io.enq
    cpifB.rx <> fifoA.io.deq
    cpifB.tx <> fifoB.io.enq
    cpifA.rx <> fifoB.io.deq
  }

  it should "Send and receive one word every 2 clock cycles" in {
    test(new MyModule2()) { d =>
      d.clock.step(2)
      val sndHelper = new MemoryMappedIOHelper(d.io.cpA, d.clock)
      val rcvHelper = new MemoryMappedIOHelper(d.io.cpB, d.clock)

      fork {
        for (i <- 0 until 200) assert(rcvHelper.rcvWithCheck() == i + 1)
        println("rcv " + rcvHelper.getClockCnt)
      }
      for (i <- 0 until 100) sndHelper.sndWithCheck(i + 1)
      println("withCheck: clock cycles to send 100 packets: " + sndHelper.getClockCnt)
    }
  }

  it should "Should be able to run in full speed" in {
    test(new MyModule2()) { d =>
      d.clock.step(2)
      val sndHelper = new MemoryMappedIOHelper(d.io.cpA, d.clock)
      val rcvHelper = new MemoryMappedIOHelper(d.io.cpB, d.clock)

      fork {
        for (i <- 0 until 100) assert(rcvHelper.receive == i + 1)
        println("rcv " + rcvHelper.getClockCnt)
      }
      for (i <- 0 until 100) sndHelper.send(i + 1)
      println("Send without check: clock cycles to send 100 packets: " + sndHelper.getClockCnt)
    }
  }

  it should "Should do HW handshake when rcv is slowing down" in {
    test(new MyModule2()) { d =>
      d.clock.step(2)
      val sndHelper = new MemoryMappedIOHelper(d.io.cpA, d.clock)
      val rcvHelper = new MemoryMappedIOHelper(d.io.cpB, d.clock)

      fork {
        for (i <- 0 until 100) {
          rcvHelper.step(2)
          assert(rcvHelper.receive == i + 1)
        }
        println("rcv " + rcvHelper.getClockCnt)
      }
      for (i <- 0 until 100) sndHelper.send(i + 1)
      println("Send without check: slow rcv, clock cycles to send 100 packets: " + sndHelper.getClockCnt)
    }
  }

  it should "Should do HW handshake when send is slowing down" in {
    test(new MyModule2()) { d =>
      d.clock.step(2)
      val sndHelper = new MemoryMappedIOHelper(d.io.cpA, d.clock)
      val rcvHelper = new MemoryMappedIOHelper(d.io.cpB, d.clock)

      fork {
        for (i <- 0 until 100) assert(rcvHelper.receive == i + 1)
        println("rcv " + rcvHelper.getClockCnt)
      }

      for (i <- 0 until 100) {
        sndHelper.step(2)
        sndHelper.send(i + 1)
      }
      println("Send without check: slow send, clock cycles to send 100 packets: " + sndHelper.getClockCnt)
    }
  }

  // Connect a CPU interface to a FIFO with Entry
  class MyModule3() extends PipeConDevice(4) {
    val cpif = Module(new PipeConDeviceRV(4, Entry(UInt(32.W)), true))
    val fifo = Module(new BubbleFifo(Entry(UInt(32.W)), 4))
    cpuPort <> cpif.cpuPort
    cpif.tx <> fifo.io.enq
    cpif.rx <> fifo.io.deq
  }

  it should "work with a S4NOC Entry" in {
    test(new MyModule3()) {
      d => {
        val helper = new MemoryMappedIOHelper(d.cpuPort, d.clock)

        d.clock.step()
        d.cpuPort.ack.expect(false.B)
        helper.write(8, 0x12)
        helper.write(4, 0x34)
        // should come back on the RX port
        assert(helper.read(4) == 0x34)
        assert(helper.read(8) == 0x12)
      }
    }
  }
}