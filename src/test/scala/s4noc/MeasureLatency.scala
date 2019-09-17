package s4noc

import chisel3._

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
}