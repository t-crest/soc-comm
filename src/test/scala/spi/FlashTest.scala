package spi

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import spi.SerialSpiTest.spi

class FlashTest() extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FlashTest"

  it should "test the flash" in {

    var spi: SerialSpiTest = null
    try {
      spi = new SerialSpiTest(1)
    } catch {
      case e: Exception => {
        println("Serial port not available")
        cancel()
      }
    }

    test(new SpiMaster) { c =>

      def echoPins() = {
        val sck = c.spi.sclk.peekInt()
        val mosi = c.spi.mosi.peekInt()
        val ncs = c.spi.ncs.peekInt()
        // println(s"ncs: $ncs, mosi: $mosi, sck: $sck")
        val bits = (ncs << 2) | (mosi << 1) | sck
        val s = "w4" + (bits + '0').toChar + "4\r"
        spi.writeReadSerial(s)
        val rx = spi.writeReadSerial("r")
        // '8' is MISO bit set
        val bit = if (rx(8 - 1) == '8') 1 else 0
        // println("rx: " + rx)
        // println("bit: " + bit)
        c.spi.miso.poke(bit)
      }
      def readByte(addr: Int) = {
        c.io.readData.ready.poke(true.B)

        c.io.readAddr.valid.poke(true.B)
        c.io.readAddr.bits.poke(addr.U)
        echoPins()
        c.clock.step()
        /*
        while(!c.io.readAddr.ready.peekBoolean()) {
          c.io.readAddr.valid.poke(true.B)
          echoPins()
          c.clock.step()
        }
        c.io.readAddr.valid.poke(false.B)

         */
        var ready = false
        var ch = ' '
        while(!ready) {
          echoPins()
          c.clock.step()
          if (c.io.readData.valid.peekBoolean()) {
            ready = true
            ch = c.io.readData.bits.peekInt().toChar
          }
        }
        c.clock.step()
        ch
      }
      for (i <- 0 until 10) {
        val c = readByte(i)
        print(c)
      }
      println()
    }
  }

}
