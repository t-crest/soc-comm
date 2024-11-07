/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 */

package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/**
 * Test the router by printing out the value at each clock cycle
 * and checking some known end values.
 */

class RouterTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Router"

  it should "give known values" in {
    test(new S4Router(Schedule(2).schedule, UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.step() // Schedule starts one clock cycle later
      for (i <- 0 until 8) {
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
        println(f"${c.io.ports(0).out.data.peekInt()}%02x ${c.io.ports(0).out.valid.peekInt()}")
      }
      // TODO: these are NOT manually verified, but from the printout
      c.io.ports(0).out.data.expect(0x57.U)
      c.io.ports(4).out.data.expect(71.U)
    }
  }
}