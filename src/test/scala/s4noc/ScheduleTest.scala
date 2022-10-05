/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 * 
 */

package s4noc
import org.scalatest.flatspec.AnyFlatSpec

import Const._

class ScheduleTest extends AnyFlatSpec {
  behavior of "Schedule creation"

  it should "pass" in {
    val sref = Schedule.gen2x2Schedule()
    println(ScheduleTable.FourNodes)
    val stest = Schedule(2).schedule
    for (i <- 0 until sref.length) {
      val slotref = sref(i)
      val slottest = stest(i)
      for (j <- 0 until slotref.length) {
        val s = slottest(j)
        val v = if (s == -1) 0 else s

        assert(slotref(j) == v)
      }
    }
  }

  it should "move correctly" in {
    val s = Schedule(3)

    assert(s.move(0, EAST) == 1)
    assert(s.move(0, WEST) == 2)
    assert(s.move(0, NORTH) == 6)
    assert(s.move(0, SOUTH) == 3)
    assert(s.move(4, EAST) == 5)
    assert(s.move(4, WEST) == 3)
    assert(s.move(4, NORTH) == 1)
    assert(s.move(4, SOUTH) == 7)
    assert(s.move(8, EAST) == 6)
    assert(s.move(8, WEST) == 7)
    assert(s.move(8, NORTH) == 5)
    assert(s.move(8, SOUTH) == 2)
  }

  it should "schedule correctly" in {
    val s = Schedule(2)

    assert(s.timeToDest(0,0) == Destination(3, 3))
    assert(s.timeToDest(0,1) == Destination(-1, 0))
    assert(s.timeToDest(0,2) == Destination(2, 2))
    assert(s.timeToDest(0,3) == Destination(1, 2))
    assert(s.timeToDest(0,4) == Destination(-1, 0))
    assert(s.timeToDest(3,0) == Destination(0, 3))
    assert(s.timeToDest(3,1) == Destination(-1, 0))
    assert(s.timeToDest(3,2) == Destination(1, 2))
    assert(s.timeToDest(3,3) == Destination(2, 2))
    assert(s.timeToDest(3,4) == Destination(-1, 0))
  }

  it should "find the time slot from src and dst" in {
    val s = Schedule(2)
    assert(s.coreToTimeSlot(0, 3) == 0)
    assert(s.coreToTimeSlot(0, 2) == 2)
    assert(s.coreToTimeSlot(0, 1) == 3)
    assert(s.coreToTimeSlot(3, 0) == 0)
    assert(s.coreToTimeSlot(3, 1) == 2)
    assert(s.coreToTimeSlot(3, 2) == 3)
  }
}
