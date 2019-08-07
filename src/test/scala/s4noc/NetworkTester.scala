/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 */

package s4noc

import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 * Test a 2x2 Network.
 */

//class NetworkTester(dut: NetworkOfFour) extends Tester(dut) {

class NetworkTester(dut: NetworkOfFour) extends PeekPokeTester(dut) {

  // after clock cycle 6 all outputs are 0, strange
  // for (i <- 0 until 8) {
  for (i <- 0 until 6) {
    for (j <- 0 until 4) {
      poke(dut.io.local(j).in.data, 0x10 * (j + 1) + i)
      poke(dut.io.local(j).in.valid, 1)
    }
    step(1)
    println(peek(dut.io.local).toString())
  }
  expect(dut.io.local(0).out.data, 0x24)
}

object NetworkTester {
  def main(args: Array[String]): Unit = {
    iotesters.Driver.execute(Array[String](), () => new NetworkOfFour()) { c => new NetworkTester(c) }
    /*
    chiselMainTest(Array("--genHarness", "--test", "--backend", "c",
      "--compile", "--targetDir", "generated"),
      () => Module(new NetworkOfFour())) {
        c => new NetworkTester(c)
      }

     */
  }
}
