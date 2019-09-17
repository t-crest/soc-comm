package s4noc

import chisel3._
import chisel3.tester._


// shall go
import chisel3.iotesters.PeekPokeTester

class MeasureLatency(dut: S4noc) extends PeekPokeTester(dut) {


    poke(dut.io.cpuPorts(0).wrData, "hcafebabe".U)
    poke(dut.io.cpuPorts(0).addr, 0)
    poke(dut.io.cpuPorts(0).wr, 1)
    step(1)

  for (i <- 0 until 20) {
    println(peek(dut.io.cpuPorts(3).rdData).toString())
    step(1)
  }
}

object MeasureLatency extends App {
  for (i <- 2 until 5) {
    iotesters.Driver.execute(Array[String](), () => new S4noc(4, i, i, 32)) { c => new MeasureLatency(c) }
  }

  def myTest(dut: S4noc): Unit = {
    println("in myTest")
    dut.io.cpuPorts(0).wrData.poke("hcafebabe".U)
    dut.io.cpuPorts(0).addr.poke(0.U)
    dut.io.cpuPorts(0).wr.poke(true.B)
    dut.clock.step(1)

    for (i <- 0 until 20) {
      println(dut.io.cpuPorts(3).rdData.peek().litValue())
      dut.clock.step(1)
    }
  }

  for (i <- 2 until 5) {
    RawTester.test(new S4noc(4, i, i, 32)) { myTest }
  }

}