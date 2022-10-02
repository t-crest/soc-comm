// package s4noc

import chisel3._
import chiseltest._

// import NocTester._

/*

object MeasureLatency extends App {

def singlePacketCpu(dut: S4NoCIO): Unit = {

  val cores = dut.s4noc.dim * dut.s4noc.dim
  println("result: Testing " + cores + " cores")
  dut.io.cpuPorts(0).wrData.poke("hcafebabe".U)
  dut.io.cpuPorts(0).addr.poke(0.U)
  dut.io.cpuPorts(0).wr.poke(true.B)
  val start = dut.io.cycCnt.peek.litValue().toInt
  dut.clock.step(1)
  dut.io.cpuPorts(0).wr.poke(false.B)

  var latency = 0
  for (i <- 0 until 40) {
    // println(dut.io.cpuPorts(3).rdData.peek.litValue())
    for (i <- 1 until cores) {
      if (latency == 0) {
        if (dut.io.cpuPorts(i).rdData.peek.litValue().toInt == 0xcafebabe) {
          latency = dut.io.cycCnt.peek.litValue().toInt - start
          println("result: Latency is " + latency)
        }
      }
    }
    dut.clock.step(1)
  }
}

val CNT = 50

def multiFlowCpu(dut: S4NoCIO): Unit = {

  val cores = dut.s4noc.dim * dut.s4noc.dim
  val valid = Schedule.getSchedule(dut.s4noc.dim)._2
  valid.foreach(print)
  println()

  // the writer threads
  for (i <- 0 until cores) {
    fork {
      // we will write as fast as possible, asserting on a FIFO that is not free
      // But writes only any other clock cycle, due to write taking 2 cycles.
      // Fits for the bubble FIFO with bandwidth 2 cycles per word
      for (cnt <- 0 until CNT) {
        for (slot <- 0 until valid.length) {
          println(s"core $i ${valid(slot)}")
          if (valid(slot)) {
            val data = ((i << 16) + dut.io.cycCnt.peek.litValue().toInt).U
            val free = write(dut.io.cpuPorts(i), slot.U, data, dut.clock)
            // assert(free, s"send FIFO for core $i was not free")
          }
        }
      }
    }
  }

  // the reader threads
  // does not work, as it pokes to the same interface as the writer threads...
  // TODO: do the raw interface for S4NoC
  for (i <- 0 until cores) {
    fork {
      // Each core should get CNT packets
      var cnt = 0
      while (cnt < CNT) {
        /*
        val packet = read(dut.io.cpuPorts(i), dut.clock)
        println(s"core $i $packet")
        if (packet._1) {
          cnt += 1
        }

         */
        dut.clock.step(100)
      }
    }
  }


  // let the TB run for some cycles
  dut.clock.step(CNT * 10)
}

def send[T](port: NetworkPort[T], data: T, slot: UInt, clock: Clock): Boolean = {

  val bufferFree = port.tx.ready.peek.litToBoolean
  /*
  val e = new Entry(data)

  // TODO: don't know yet how this works
  val x = e.one
  e.getDt
  val y = e.getDt
  //e.data = 123.asInstanceOf[e.data.type]
  port.tx.bits.poke(e)
  // port.tx.bits.data.poke(e.data)

   */
  port.tx.bits.time.poke(slot)
  if (bufferFree) {
    port.tx.valid.poke(true.B)
  }
  clock.step(1)
  port.tx.valid.poke(false.B)
  bufferFree
}

def receive[T](port: NetworkPort[T], clock: Clock) = {
  val dataAvailable = port.rx.valid.peek.litToBoolean
  var data = 0
  var from = 0
  if (dataAvailable) {
    data = port.rx.bits.data.peek.litValue.toInt
    from = port.rx.bits.time.peek.litValue.toInt
    port.rx.ready.poke(true.B)
  }
  clock.step(1)
  port.rx.ready.poke(false.B)
  (dataAvailable, data, from)
}

def singleFlow(dut: S4NoC) = {

  println(s"result: testing one cores")

  fork {
    for (cnt <- 0 until CNT) {
      val data = dut.io.cycCnt.peek
      while (!send(dut.io.networkPort(0), data, 0.U, dut.clock)) {}
    }
  }

  for (cnt <- 0 until CNT) {
    var loop = true
    while(loop) {
      val ret = receive(dut.io.networkPort(3), dut.clock)
      loop = !ret._1
      if (ret._1) {
        val latency = dut.io.cycCnt.peek.litValue().toInt - ret._2
        println(s"result: received $ret latency is $latency")
      }
    }
  }
}
def multiFlow(dut: S4NoC) = {

  val cores = dut.dim * dut.dim
  println(s"result: testing $cores cores")
  val valid = Schedule.getSchedule(dut.dim)._2

  // the writer threads
  for (i <- 0 until cores) {
    fork {
      // We will write as fast as possible, which is max one word per 2 cc (current FIFO).
      // We may also have head of line blocking.
      for (cnt <- 0 until CNT) {
        for (slot <- 0 until valid.length) {
          // println(s"core $i ${valid(slot)}")
          if (valid(slot)) {
            val data = ((i << 16) + dut.io.cycCnt.peek.litValue().toInt).U
            while (!send(dut.io.networkPort(i), data, slot.U, dut.clock)) {}
          }
        }
      }
    }
  }

  for (i <- 0 until cores) {
    fork {
      // Each core should get CNT packets
      var cnt = 0
      while (cnt < CNT) {
        val packet = receive(dut.io.networkPort(i), dut.clock)
        if (packet._1) {
          val now = dut.io.cycCnt.peek.litValue().toInt
          val ts = packet._2 & 0xffff
          val sender = packet._2 >> 16
          println(s"result: latency is ${now-ts} (sent at $ts from $sender to $i)")
          cnt += 1
        }
      }
    }
  }

  dut.clock.step(CNT*10)
}

// RawTester.test(new S4NoC(4,2,2,32)) { singleFlow }
println("result: 2 elements")
RawTester.test(new S4NoC(4,2,2,32)) { multiFlow }
println("result: 4 elements")
RawTester.test(new S4NoC(4,4,4,32)) { multiFlow }
println("result: 2 elements")
RawTester.test(new S4NoC(16,2,2,32)) { multiFlow }
println("result: 4 elements")
RawTester.test(new S4NoC(16,4,4,32)) { multiFlow }

 */

    /*
    RawTester.test(new S4NoCIO(4, 2, 2, 32)) { multiFlowCpu }

    for (i <- 2 until 7) {
      println(s"result: Using bubble FIFOs with $i elements")
      RawTester.test(new S4NoCIO(4, i, i, 32)) { singleFlowCpu }
    }
    for (i <- 2 until 7) {
      println(s"result: Using bubble FIFOs with $i elements")
      RawTester.test(new S4NoCIO(9, i, i, 32)) { singleFlowCpu }
    }
    for (i <- 2 until 7) {
      println(s"result: Using bubble FIFOs with $i elements")
      RawTester.test(new S4NoCIO(16, i, i, 32)) { singleFlowCpu }
    }
}
     */