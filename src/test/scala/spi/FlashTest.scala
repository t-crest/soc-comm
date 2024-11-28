package spi

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import spi.SerialSpiTest.spi

class FlashTest() extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FlashTest"

  it should "test the flash" in {

    var spiDriver: SerialSpiTest = null
    try {
      spiDriver = new SerialSpiTest(1)
    } catch {
      case e: Exception => {
        println("Serial port not available")
        cancel()
      }
    }

    test(new SpiMaster) { c =>

      /*
      def echoPins() = {
        val sck = c.spi.sclk.peekInt().toInt
        val mosi = c.spi.mosi.peekInt().toInt
        val ncs = c.spi.ncs.peekInt().toInt
        val bit = spiDriver.echoPinsOuter(sck, mosi, ncs)
        c.spi.miso.poke(bit)
      }

       */
      def readWord(addr: Int) = {
        c.io.readData.ready.poke(true.B)

        c.io.readAddr.valid.poke(true.B)
        c.io.readAddr.bits.poke(addr.U)
        spiDriver.echoPins(c.spi)
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
        var v = 0
        while(!ready) {
          spiDriver.echoPins(c.spi)
          c.clock.step()
          if (c.io.readData.valid.peekBoolean()) {
            ready = true
            v = c.io.readData.bits.peekInt().toInt
          }
        }
        c.clock.step()
        v
      }
      for (i <- 0 until 3) {
        val v = readWord(i*4)
        println(f"Read 0x$v%08x")
      }
    }
  }

}
