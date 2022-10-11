package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import NocTest._

/**
  * Do some performance tests.
  */
class SystemTest extends AnyFlatSpec with ChiselScalatestTester {

  // this does not work, it "must be inside Builder context"
  // val noc = new S4noc(4, 2, 2, 32)

  behavior of "Dummy"

  /*
  it should "just work" in {
    test(new S4noc(4, 2, 2, 32)) { d =>
      println(d.getClass)
    }
  }

   */

  behavior of "S4NoC"

  val CNT = 500
  val data = new Array[Int](CNT)
  val rnd = new scala.util.Random()
  for (i <- 0 until CNT) data(i) = rnd.nextInt()

  it should "not overrun the channel" in {
    test(new S4NoCTopCombOld(Config(4, 2, 2, 2, 32))) { d =>

      val th = fork {
        for (i <- 0 until CNT) {
          blockingWrite(d.io.cpuPorts(0), 0.U, (data(i).toLong & 0xffffffffL).U, d.clock)
        }
      }

      for (i <- 0 until CNT) {
        var cont = true
        do {
          val ret = read(d.io.cpuPorts(3), d.clock)
          // println("cycle: " + d.io.cycCnt.peek.litValue + " read " + i + " " + ret)
          cont = !ret._1

          if (ret._1) assert(ret._2 == data(i))
        } while (cont)
      }
      println("" + CNT + " words sent")
      println("Bandwidth = " + (d.io.cycCnt.peek.litValue.toFloat / CNT) + " clock cycles per word")

      th.join()
    }
  }

  it should "measure the latency" in {
    test(new S4NoCTopCombOld(Config(4, 2, 2, 2, 32))) { d =>

      val th = fork {
        for (i <- 0 until CNT) {
          val data = d.io.cycCnt.peek()
          blockingWrite(d.io.cpuPorts(0), 0.U, d.io.cycCnt.peek, d.clock)
        }
      }

      var min = 1000
      var max = 0
      for (i <- 0 until CNT) {
        var cont = true
        do {
          val ret = read(d.io.cpuPorts(3), d.clock)
          if (ret._1) {
            val cyc = d.io.cycCnt.peek.litValue
            val lat = (cyc - ret._2).toInt
            min = if (lat < min) lat else min
            max = if (lat > max) lat else max
            // println("cycle: " + cyc + " read " + i + " " + ret + " latency: " + (cyc - ret._2))

          }
          cont = !ret._1
        } while (cont)
      }
      println("" + CNT + " words sent")
      println("Latency: min = " + min + " max = " + max)

      th.join()
    }
  }
}
