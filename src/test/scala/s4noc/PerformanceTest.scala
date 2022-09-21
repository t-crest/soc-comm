package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable._

class PerformanceTest extends AnyFlatSpec with ChiselScalatestTester {

  val n = 4

  def printScheduleInfo(sched: Schedule) = {
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

  "S4NoC" should "be fast ;-)" in {
    test(new Network(n, UInt(32.W))) { d =>

      var countCycles = 0
      def tick() = {
        d.clock.step()
        countCycles += 1
      }

      val sched = Schedule(n)
      // printScheduleInfo(sched)
      val t = new TrafficGen(n * n)
      var slotCnt = 0

      def runIt(count: Int) = {

        var min = 10000
        var max = 0
        var cnt = 0
        var sum = 0

        for (i <- 0 until count) {
          // println(s"clock cycle #: $countCycles")
          t.tick()
          for (core <- 0 until n * n) {
            val local = d.io.local(core)
            // send
            local.in.valid.poke(false.B)
            val dest = sched.timeToDest(core, slotCnt)._1
            // println(s"$slotCnt: core $core can send to $dest")
            if (dest != -1) {
              val data = t.getValue(core, dest)
              // printInfo(data)
              if (data != -1) {
                local.in.data.poke(data)
                local.in.valid.poke(true.B)
              }
            }
            // receive
            // TODO: sanity check for correct routing
            if (local.out.valid.peekBoolean()) {
              val recv = local.out.data.peekInt().toInt
              val latency = countCycles - (recv & 0x0ffff)
              if (latency < min) min = latency
              if (latency > max) max = latency
              cnt += 1
              sum += latency
              // println(s"Received with latency of $latency")
              // printInfo(recv)
            }
          }

          tick()
          slotCnt = (slotCnt + 1) % sched.len
        }
        (cnt, sum.toDouble/cnt, min, max)
      }

      for (rate <- 1 until 70) {
        t.injectionRate = rate.toDouble / 100
        t.dropped = 0
        val (cnt, avg, min, max) = runIt(1000)
        // TODO: sanity check of cnt, nr of cores, and injection rate
        // println(s"injection rate: ${t.injectionRate} cnt: $cnt dropped: ${t.dropped} avg: $avg, min: $min, max: $max")
        println(s"(${t.injectionRate}, $avg)")
      }

    }
  }
}

