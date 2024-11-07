package s4noc

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import soc._

/**
  * Do some performance/latency tests.
  */
class LatencyTest(dontRun: String) extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "S4NoC"

  val CNT = 500
  val data = new Array[Int](CNT)
  val rnd = new scala.util.Random()
  for (i <- 0 until CNT) data(i) = rnd.nextInt()

  it should "not overrun the channel" in {
    test(new S4NoCTop(Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32))) { d =>

      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)
      val th = fork {
        for (i <- 0 until CNT) {
          helpSnd.send(data(i).toLong & 0xffffffffL)
        }
      }

      for (i <- 0 until CNT) {
        val d = helpRcv.receive
        assert(d.toInt == data(i))
      }
      println("" + CNT + " words sent")
      println("Bandwidth = " + (d.io.cycCnt.peekInt().toFloat / CNT) + " clock cycles per word")

      th.join()
    }
  }

  it should "measure the latency" in {
    test(new S4NoCTop(Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32))) { d =>

      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)

      val th = fork {
        for (i <- 0 until CNT) {
          val data = helpSnd.getClockCnt
          helpSnd.send(data)
        }
      }

      var min = 1000
      var max = 0
      for (i <- 0 until CNT) {
        val d = helpRcv.receive
        val cyc = helpRcv.getClockCnt
        val lat = (cyc - d).toInt
        min = if (lat < min) lat else min
        max = if (lat > max) lat else max
        // println("cycle: " + cyc + " read " + i + " " + ret + " latency: " + (cyc - ret._2))
      }
      println("" + CNT + " words sent")
      println("Latency: min = " + min + " max = " + max)

      th.join()
    }
  }
}
