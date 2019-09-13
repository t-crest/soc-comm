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

class RouterTester extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Router"

  it should "give known values" in {
    test(new S4Router(Schedule.getSchedule(2)._1, UInt(16.W))) { c =>
      for (i <- 0 until 5) {
        c.io.ports(0).in.data.poke((0x10 + i).U)
        c.io.ports(1).in.data.poke((0x20 + i).U)
        c.io.ports(2).in.data.poke((0x30 + i).U)
        c.io.ports(3).in.data.poke((0x40 + i).U)
        c.io.ports(4).in.data.poke((0x50 + i).U)

        c.io.ports(0).in.valid.poke(true.B)
        c.io.ports(1).in.valid.poke(true.B)
        c.io.ports(2).in.valid.poke(true.B)
        c.io.ports(3).in.valid.poke(true.B)
        c.io.ports(4).in.valid.poke(true.B)
        c.clock.step(1)
        println(c.io.ports(0).out.data.peek.litValue().toInt.toString())
      }

      c.io.ports(0).out.data.expect(0x14.U)
      c.io.ports(4).out.data.expect(0x34.U)
    }
  }
}