package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable._

class PerformanceTest extends AnyFlatSpec with ChiselScalatestTester {

  val n = 2
  "S4NoC" should "be fast ;-)" in {
    test(new Network(n, UInt(32.W))) { d =>

      var countCycles = 0
      def tick() = {
        d.clock.step()
        countCycles += 1
      }

      val sched = Schedule(n)
      println(sched)
      for (i <- 0 until n * n) {
        println(s"From core $i")
        for (j <- 0 until sched.schedule.length) {
          val dest = sched.timeToDest(i, j)._1
          println(s"  at timeslot $j core $i reaches $dest")
        }
        for (j <- 0 until n * n) {
          val slot = sched.coreToTimeSlot(i, j)
          println(s"  use slot $slot to reach code $j")
        }
      }

      def printInfo(data: Int) = {
        val from = data >> 24
        val to = (data >> 16) & 0x0ff
        val cnt = data & 0x0ffff
        if (data == -1) {
          println("Have -1")
        } else {
          println(s"Have from $from to $to inserted at $cnt")
        }
      }

      val t = new TrafficGen(n * n)
      var slotCnt = 0
      for (i <- 0 until 10) {
        t.tick()
        for (core <- 0 until n * n) {
          val dest = sched.timeToDest(core, slotCnt)._1
          println(s"$slotCnt: core $core can send to $dest")
          if (dest != -1) {
            val data = t.getValue(core, dest)
            printInfo(data)
          }
        }
        
        tick()
        slotCnt = (slotCnt + 1) % sched.len
        println(s"slot #: $slotCnt")
      }

    }
  }
}

