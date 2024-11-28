package spi

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FlashTest() extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FlashTest"

  it should "test the flash" in {

    var spiDriver: SerialSpiDriver = null
    try {
      spiDriver = new SerialSpiDriver(1)
    } catch {
      case e: Exception => {
        println("Serial port not available")
        cancel()
      }
    }

    test(new SpiMaster) { c =>

      def readWord(addr: Int): Int = {
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
