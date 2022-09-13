/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 * 
 * Compare a program connected 2x2 with a manual connected 2x2 network.
 */

package s4noc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


import Const._

/**
 * A manually connected 2x2 NoC for testing.
 */
class NetworkOfFour() extends Module {
  val io = IO(new Bundle {
    val local = Vec(4, new ChannelIO(UInt(32.W)))
  })

  val schedule = Schedule(2)
  val net = new Array[S4Router[UInt]](4)
  for (i <- 0 until 4) {
    net(i) = Module(new S4Router(schedule.schedule, UInt(32.W)))
    io.local(i).out := net(i).io.ports(LOCAL).out
    net(i).io.ports(LOCAL).in := io.local(i).in
  }

  // Router indexes:
  // 0 - 1
  // |   |
  // 2 - 3
  def connect(r1: Int, p1: Int, r2: Int, p2: Int): Unit = {
    net(r1).io.ports(p1).in := net(r2).io.ports(p2).out
    net(r2).io.ports(p2).in := net(r1).io.ports(p1).out
  }

  connect(0, NORTH, 2, SOUTH)
  connect(0, EAST, 1, WEST)
  connect(0, SOUTH, 2, NORTH)
  connect(0, WEST, 1, EAST)

  connect(1, NORTH, 3, SOUTH)
  connect(1, SOUTH, 3, NORTH)

  connect(2, EAST, 3, WEST)
  connect(2, WEST, 3, EAST)
}

class TwoNetworks() extends Module {
  val io = IO(new Bundle {
    val toNocA = Vec(4, new ChannelIO(UInt(32.W)))
    val toNocB = Vec(4, new ChannelIO(UInt(32.W)))
  })

  val na = Module(new NetworkOfFour())
  val nb = Module(new Network(2, UInt(32.W)))
  for (i <- 0 until 4) {
    io.toNocA(i).out := na.io.local(i).out
    na.io.local(i).in := io.toNocA(i).in
    io.toNocB(i).out := nb.io.local(i).out
    nb.io.local(i).in := io.toNocB(i).in
  }
}


/**
 * Compare two 2x2 networks.
 */

class NetworkCompare extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Compare two 2x2 networks"

  "NoCs" should "be identical" in {
    test(new TwoNetworks()) { dut =>
      for (i <- 0 until 6) {
        for (j <- 0 until 4) {
          dut.io.toNocA(j).in.data.poke((0x10*(j+1) +i).U)
          dut.io.toNocB(j).in.data.poke((0x10*(j+1) +i).U)
        }
        dut.clock.step(1)
        for (j <- 0 until 4) {
          dut.io.toNocA(j).out.data.expect(dut.io.toNocB(j).out.data.peek())
        }
      }
      dut.io.toNocA(0).out.data.expect(0x24.U)
      dut.io.toNocB(0).out.data.expect(0x24.U)
    }

  }
}