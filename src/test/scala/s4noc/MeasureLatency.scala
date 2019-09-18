package s4noc

import chisel3._
import chisel3.tester._

import NocTester._

object MeasureLatency extends App {

  def singleFlow(dut: S4noc): Unit = {

    val cores = dut.dim * dut.dim
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

  val CNT = 5

  def multiFlow(dut: S4noc): Unit = {

    val cores = dut.dim * dut.dim
    val valid = Schedule.getSchedule(dut.dim)._2
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

  RawTester.test(new S4noc(4, 2, 2, 32)) { multiFlow }

  for (i <- 2 until 7) {
    println(s"result: Using bubble FIFOs with $i elements")
    RawTester.test(new S4noc(4, i, i, 32)) { singleFlow }
  }
  for (i <- 2 until 7) {
    println(s"result: Using bubble FIFOs with $i elements")
    RawTester.test(new S4noc(9, i, i, 32)) { singleFlow }
  }
  for (i <- 2 until 7) {
    println(s"result: Using bubble FIFOs with $i elements")
    RawTester.test(new S4noc(16, i, i, 32)) { singleFlow }
  }

}