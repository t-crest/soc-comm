package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable._

class PerformanceTest extends AnyFlatSpec with ChiselScalatestTester {

  "S4NoC" should "be fast ;-)" in {
    test(new Network(2, UInt(32.W))) { d =>
      val sched = Schedule(2)
      println(sched)
      for (i <- 0 until 4) {
        println(s"From core $i")
        for (j <- 0 until sched.schedule.length) {
          val dest = sched.timeToDest(i, j)
          println(s"  at timeslot $j it reaches $dest")
        }
        for (j <- 0 until 4) {
          val slot = sched.coreToTimeSlot(i, j)
          println(s"  use slot $slot to reach code $j")
        }
      }

      val t = new TrafficGen(2)

    }
  }
}

