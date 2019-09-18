package s4noc

import chisel3._
import chisel3.tester._

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