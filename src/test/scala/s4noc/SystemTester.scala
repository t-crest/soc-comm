package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

import NocTester._

/**
  * Do some performance tests.
  */
class SystemTester extends FlatSpec with ChiselScalatestTester with Matchers {

  // this does not work, it "must be inside Builder context"
  // val noc = new S4noc(4, 2, 2, 32)

  behavior of "The S4NOC:"

  /*
  it should "just work" in {
    test(new S4noc(4, 2, 2, 32)) { d =>
      println(d.getClass)
    }
  }

   */

  behavior of "Bandwidth"

  val CNT = 20
  val data = new Array[Int](CNT)
  val rnd = new scala.util.Random()
  for (i <- 0 until CNT) data(i) = rnd.nextInt()

  it should "not overrun the channel" in {
    test(new S4noc(4, 2, 2, 32)) { d =>

      val th = fork {
        for (i <- 0 until CNT) {
          blockingWrite(d.io.cpuPorts(0), 0.U, (data(i).toLong & 0xffffffffL).U, d.clock)
        }
      }

      for (i <- 0 until CNT) {
        var cont = true
        do {
          val ret = read(d.io.cpuPorts(3), d.clock)
          println("cycle: " + d.io.cycCnt.peek.litValue + " read " + i + " " + ret)
          cont = !ret._1

          if (ret._1) assert(ret._2 == data(i))
        } while (cont)
      }
      println("" + CNT + " words sent")
      println("Bandwidth = " + (d.io.cycCnt.peek.litValue.toFloat / CNT) + " clock cycles per word")

      th.join()
    }
  }

  it should "test the latency" in {
    test(new S4noc(4, 2, 2, 32)) { d =>

      val th = fork {
        for (i <- 0 until CNT) {
          val data = d.io.cycCnt.peek()
          blockingWrite(d.io.cpuPorts(0), 0.U, d.io.cycCnt.peek, d.clock)
        }
      }

      for (i <- 0 until CNT) {
        var cont = true
        do {
          val ret = read(d.io.cpuPorts(3), d.clock)
          if (ret._1) {
            val cyc = d.io.cycCnt.peek.litValue
            println("cycle: " + cyc + " read " + i + " " + ret + " latency: " + (cyc - ret._2))

          }
          cont = !ret._1
        } while (cont)
      }


      th.join()
    }
  }


}
