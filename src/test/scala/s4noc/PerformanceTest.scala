package s4noc

import chisel3._
import chiseltest._
import scala.collection.mutable._

object PerformanceTest extends App {

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
  var slotCnt = 0

  /**
    * This is the interface directly to the network without an NI, or a CPU interface.
    */
  RawTester.test(new Network(n, UInt(32.W))) { d =>
    var countCycles = 0

    def tick() = {
      d.clock.step()
      countCycles += 1
    }

    def runIt(count: Int, drain: Int) = {

      var injected = 0 // into the NoC
      var received = 0 // from the NoC
      var min = 10000
      var max = 0
      var sum = 0
      t.reset()

      for (i <- 0 until (count + drain)) {
        // println(s"clock cycle #: $countCycles")
        t.tick(i < count)
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
          // TODO: sanity check for correct routing
          if (local.out.valid.peekBoolean()) {
            val recv = local.out.data.peekInt().toInt
            val latency = countCycles - (recv & 0x0ffff)
            val to = (recv >> 16) & 0x0ff
            // assert(to == core, s"$to should be $core")
            if (latency < min) min = latency
            if (latency > max) max = latency
            received += 1
            sum += latency
            // println(s"Received with latency of $latency")
            // printInfo(recv)
          }
        }

        tick()
        slotCnt = (slotCnt + 1) % sched.len
      }
      (injected, received, sum.toDouble / received, min, max)
    }

    println(s"${n * n} cores with ideal queues")
    val count = 10000
    val drain = 20
    for (rate <- 1 until 11 by 10) { // ?? is max
      t.injectionRate = rate.toDouble / 100
      val (injected, received, avg, min, max) = runIt(count, drain)
      val effectiveInjectionRate = injected.toDouble / count / (n * n)
      // TODO: sanity check of received, nr of cores, and injection rate
      println(s"inserted ${t.inserted} injected: $injected received: $received requested injection rate: ${t.injectionRate} effective injection rate $effectiveInjectionRate avg: $avg, min: $min, max: $max")
      println(s"($effectiveInjectionRate, $avg)")
    }
  }

  RawTester.test(new S4NoC(Config(n * n, 256, 256, 256, 32)), Seq(VerilatorBackendAnnotation,
    chiseltest.internal.NoThreadingAnnotation)) { d =>

    var countCycles = 0

    def tick() = {
      d.clock.step()
      countCycles += 1
    }

    def runIt(count: Int, drain: Int) = {

      var injected = 0 // into the NoC
      var received = 0 // from the NoC
      var min = 10000
      var max = 0
      var sum = 0
      t.inserted = 0
      t.reset()

      var destCnt = 0
      for (i <- 0 until (count + drain)) {
        // println(s"clock cycle #: $countCycles")
        t.tick(i < count) // only generate new traffic until drain cycles
        for (core <- 0 until n * n) {
          val ni = d.io.networkPort(core)
          // send
          ni.tx.valid.poke(false.B)
          if (ni.tx.ready.peekBoolean()) {
            val (data, dest) = t.getValueFromSingle(core)
            val time = sched.coreToTimeSlot(core, dest)
            // printInfo(data)
            if (data != -1) {
              ni.tx.bits.data.poke(data.U)
              ni.tx.bits.time.poke(time.U)
              ni.tx.valid.poke(true.B)
              injected += 1
            }
          }
          // receive
          ni.rx.ready.poke(true.B)
          if (ni.rx.valid.peekBoolean()) {
            val recv = ni.rx.bits.data.peek.litValue.toInt
            val to = (recv >> 16) & 0x0ff
            assert(to == core, s"$to should be $core")
            val latency = countCycles - (recv & 0x0ffff)
            // Ignore negative latency from packets from the former run
            // TODO: find a way to really drain the NoC
            if (latency > 0) {
              if (latency < min) min = latency
              if (latency > max) max = latency
              received += 1
              sum += latency
            }
          }
        }
        tick()
        slotCnt = (slotCnt + 1) % sched.len
        destCnt = (destCnt + 1) % (n * n)
      }
      (injected, received, sum.toDouble / received, min, max)
    }

    println(s"${n * n} cores with different queues")
    val count = 10000
    val drain = 1000
    d.clock.setTimeout(11000)
    for (rate <- 1 until 32 by 5) { // ?? is max
      t.reset()
      countCycles = 0
      t.injectionRate = rate.toDouble / 100
      val (injected, received, avg, min, max) = runIt(count, drain)
      val effectiveInjectionRate = injected.toDouble / count / (n * n)
      // TODO: sanity check of received, nr of cores, and injection rate
      // println(s"inserted ${t.inserted} injected: $injected received: $received requested injection rate: ${t.injectionRate} effective injection rate $effectiveInjectionRate avg: $avg, min: $min, max: $max")
      println(s"($effectiveInjectionRate, $avg)")
    }
  }
}

