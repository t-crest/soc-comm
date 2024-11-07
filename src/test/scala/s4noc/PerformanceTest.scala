package s4noc

import chisel3._
import chiseltest._
import scala.collection.mutable._

object PerformanceTest extends App {

  // n * n is the number of cores
  val n = 4

  def printScheduleInfo(sched: Schedule) = {
    println(sched)
    for (i <- 0 until n * n) {
      println(s"From core $i")
      for (j <- 0 until sched.schedule.length) {
        val dest = sched.timeToDest(i, j).dest
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

  val sched = Schedule(n)
  // printScheduleInfo(sched)
  val t = new TrafficGen(n * n)
  // schedule starts one clock cycle later
  var slotCnt = -1

  /**
    * This is the interface directly to the network without an NI, or a CPU interface.
    */
  // RawTester.test(new Network(n, UInt(32.W)), Seq(VerilatorBackendAnnotation, chiseltest.internal.NoThreadingAnnotation)
  RawTester.test(new Network(n, UInt(32.W))
    ) { d =>

    def runIt(heatUp: Int, count: Int, drain: Int) = {
      var countCycles = 0

      def tick() = {
        d.clock.step()
        countCycles += 1
      }

      var injected = 0 // into the NoC
      var received = 0 // from the NoC
      var receivedCnt = 0 // after heatup phase
      var min = 10000
      var max = 0
      var sum = 0
      t.reset()

      for (i <- 0 until (heatUp + count + drain)) {
        t.tick(i < (heatUp + count))
        for (core <- 0 until n * n) {
          val local = d.io.local(core)
          // send
          local.in.valid.poke(false.B)
          val dest = sched.timeToDest(core, slotCnt).dest
          // println(s"$slotCnt: core $core can send to $dest")
          if (dest != -1) {
            val data = t.getValue(core, dest)
            // printInfo(data)
            if (data != -1) {
              local.in.data.poke(data.U)
              local.in.valid.poke(true.B)
              injected += 1
            }
          }
          // receive
          if (local.out.valid.peekBoolean()) {
            val recv = local.out.data.peekInt().toInt
            // println(s"received ($core, $recv)")
            // printInfo(recv)
            assert(t.check.contains((core, recv)), "Value out of thin air")
            t.check.remove((core, recv))
            val latency = countCycles - (recv & 0x0ffff)
            val to = (recv >> 16) & 0x0ff
            assert(to == core, s"$to should be $core")
            received += 1
            if (i > heatUp) {
              if (latency < min) min = latency
              if (latency > max) max = latency
              sum += latency
              receivedCnt += 1
            }
            // println(s"Received $received with latency of $latency")
          }
        }

        tick()
        slotCnt = (slotCnt + 1) % sched.len
      }
      assert(t.check.size == 0, s"Check set should be empty, but is ${t.check.size}. NoC needs to be drained.")
      (injected, received, sum.toDouble / receivedCnt, min, max)
    }

    val count = 8000
    val heatUp = 4000
    val drain = 4000
    println(s"${n * n} cores with ideal queues, $heatUp heatup cycles, $count clock cycles")
    d.clock.setTimeout(10000)
    for (rate <- 25 until 26 by 3) { // for 2x2
      t.injectionRate = rate.toDouble / 100
      val (injected, received, avg, min, max) = runIt(heatUp, count, drain)
      val effectiveInjectionRate = injected.toDouble / (heatUp + count) / (n * n)
      println(s"inserted ${t.inserted} injected: $injected received: $received requested injection rate: ${t.injectionRate} effective injection rate $effectiveInjectionRate avg: $avg, min: $min, max: $max")
      // println(s"($effectiveInjectionRate, $avg)")
    }
  }


  // Maybe this should go into its own class
  // RawTester.test(new S4NoC(Config(n * n, MemType(256), MemType(26), MemType(256), 32)), Seq(VerilatorBackendAnnotation,
  RawTester.test(new S4NoC(Config(n * n, MemType(256), MemType(26), MemType(256), 32))) { d =>

    var countCycles = 0

    def tick() = {
      d.clock.step()
      countCycles += 1
    }

    def runIt(heatUp: Int, count: Int, drain: Int) = {

      var injected = 0 // into the NoC
      var received = 0 // from the NoC
      var receivedCnt = 0 // after heatup phase
      var min = 10000
      var max = 0
      var sum = 0
      t.inserted = 0
      t.reset()

      var destCnt = 0
      for (i <- 0 until (heatUp + count + drain)) {
        // println(s"clock cycle #: $countCycles")
        t.tick(i < (heatUp + count)) // only generate new traffic until drain cycles
        for (core <- 0 until n * n) {
          val ni = d.io.networkPort(core)
          // send
          ni.tx.valid.poke(false.B)
          if (ni.tx.ready.peekBoolean()) {
            val (dest, data) = t.getValueForCore(core)
            // printInfo(data)
            if (data != -1) {
              ni.tx.bits.data.poke(data.U)
              ni.tx.bits.core.poke(dest.U)
              ni.tx.valid.poke(true.B)
              injected += 1
            }
          }
          // receive
          ni.rx.ready.poke(true.B)
          if (ni.rx.valid.peekBoolean()) {
            val recv = ni.rx.bits.data.peekInt().toInt
            val to = (recv >> 16) & 0x0ff
            assert(to == core, s"$to should be $core")
            assert(t.check.contains((core, recv)), "Value out of thin air")
            t.check.remove((core, recv))
            val latency = countCycles - (recv & 0x0ffff)
            // Ignore negative latency from packets from the former run
            // TODO: find a way to really drain the NoC
            // if (latency > 0)
            assert(latency > 0, "Value from last run")
            received += 1
            if (i > heatUp) {
              if (latency < min) min = latency
              if (latency > max) max = latency
              receivedCnt += 1
              sum += latency
            }
          }
        }
        tick()
        slotCnt = (slotCnt + 1) % sched.len
        destCnt = (destCnt + 1) % (n * n)
      }
      assert(t.check.size == 0, s"Check set should be empty, but is ${t.check.size}. NoC needs to be drained.")
      (injected, received, sum.toDouble / receivedCnt, min, max)
    }

    println(s"${n * n} cores with different queues")
    val count = 8000
    val heatUp = 4000
    val drain = 20000
    d.clock.setTimeout(200000)
    // for (rate <- 1 until 30 by 3) { // ?? is max
    for (count <- 8000 to 16000 by 8000) {
      println(s"count: $count")
      val rate = 58 // 56
      t.reset()
      countCycles = 0
      t.injectionRate = rate.toDouble / 100
      val (injected, received, avg, min, max) = runIt(heatUp, count, drain)
      val effectiveInjectionRate = injected.toDouble / (heatUp + count) / (n * n)
      println(s"inserted ${t.inserted} injected: $injected received: $received requested injection rate: ${t.injectionRate} effective injection rate $effectiveInjectionRate avg: $avg, min: $min, max: $max")
      // println(s"($effectiveInjectionRate, $avg)")
    }
  }
}

