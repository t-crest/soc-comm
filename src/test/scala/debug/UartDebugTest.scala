package debug

import chisel.lib.uart._
import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class UartDebugTestTop extends Module {
  val io = IO(new Bundle {
    val inRx = new UartIO()
    val outTx = Flipped(new UartIO())
    val dout = Output(UInt(32.W))
    val din = Input(UInt(32.W))
  })

  val tx = Module(new BufferedTx(100000000, 100000000/16))
  val rx = Module(new Rx(100000000, 100000000/16))

  io.outTx <> tx.io.channel
  io.inRx <> rx.io.channel

  val dbg = Module(new UartDebug(100000000, 100000000/16))

  dbg.io.rx := tx.io.txd
  rx.io.rxd := dbg.io.tx
  io.dout := dbg.io.dout
  dbg.io.din := io.din
}

class UartDebugTest extends AnyFlatSpec with ChiselScalatestTester {
  "UartDebug" should "work" in {
    test(new UartDebugTestTop()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.din.poke(0xabcd.U)
      dut.clock.setTimeout(10001)
      dut.io.outTx.valid.poke(true.B)
      dut.io.outTx.bits.poke(0x42.U)
      dut.clock.step(10)
      dut.io.outTx.valid.poke(false.B)
      dut.clock.step(1000)
    }
  }
}
