package spi

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import spi.SerialSpiTest.spi

class FlashTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FlashTest"

  it should "test the flash" in {
    val spi = new SerialSpiTest(1)
    spi.csLow()
    print(spi.writeReadSerial("r"))
    spi.csHigh()
    test(new SpiMaster) { c =>
      for(i <- 0 until 1000) {
        val sck = c.spi.sclk.peekInt()
        val mosi = c.spi.mosi.peekInt()
        val ncs = c.spi.ncs.peekInt()
        // println(s"sck: $sck, mosi: $mosi, ncs: $ncs")
        val bits = (ncs << 2) | (mosi << 1) | sck
        val s = "w4" + (bits + '0').toChar + "4\r"
        spi.writeReadSerial(s)
        println(s)
        c.clock.step()
        // println("dout: " + c.io.dataOut.peekInt())
        if (c.io.dataReady.peekBoolean()) {
          println("Data is " + c.io.dataOut.peekInt())
        }
      }
    }
  }

}
