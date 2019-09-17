/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 */

package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._

// shall go
import chisel3.iotesters.PeekPokeTester

/**
 * Test a 2x2 Network.
 */

class NetworkTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "2x2 Network"

  "the NoC" should "work" in {
    test(new NetworkOfFour()) { dut =>
      // after clock cycle 6 all outputs are 0, strange
      // for (i <- 0 until 8) {
      for (i <- 0 until 6) {
        for (j <- 0 until 4) {
          dut.io.local(j).in.data.poke((0x10 * (j + 1) + i).U)
          dut.io.local(j).in.valid.poke(true.B)
        }
        dut.clock.step(1)
        // don't know how to do the same printout as the iotesters.PeekPokeTester
        // println(dut.io.local.peek.litValue.toString())
      }
      dut.io.local(0).out.data.expect(0x24.U)
    }
  }

}

// below should go when println for new tester is checked
class NetworkTesterOrig(dut: NetworkOfFour) extends PeekPokeTester(dut) {

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

object NetworkTesterOrig extends App {
    iotesters.Driver.execute(Array[String](), () => new NetworkOfFour()) { c => new NetworkTesterOrig(c) }
}
