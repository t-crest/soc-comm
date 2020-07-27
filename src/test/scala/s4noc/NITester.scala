/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 */

package s4noc

import chisel3._
import chisel3.tester._
import org.scalatest._


/**
 * Test the router by printing out the value at each clock cycle
 * and checking some known end values.
 */

class NITester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "NI"

  it should "work" in {
    test(new NetworkInterface(4, 2, 2, UInt(16.W))) { dut =>
      // TODO: how can I create a an Entry of generic type to be used in testing?
      // Or how do I create a bundle literal that contains an Entry with a generic type?

      // Following does not run
      val e = Wire(new Entry(UInt(16.W)))
      e.data := 1.asUInt(16.W)
      e.time := 2.asUInt
      dut.io.networkPort.tx.bits.poke(e)

      // .Lit does not work in the following, even not on 0.2-SNAPSHOT
      // dut.io.networkPort.tx.bits.poke(chiselTypeOf(dut.io.networkPort.tx.bits).Lit(1.U))

      dut.clock.step(1)
      // may take more than one clock cycle...
      dut.io.local.out.data.expect(1.U)
    }
  }
}