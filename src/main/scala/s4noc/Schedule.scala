/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 * 
 * Schedules for the S4NOC, as described in:
 * 
 * Florian Brandner and Martin Schoeberl,
 * Static Routing in Symmetric Real-Time Network-on-Chips,
 * In Proceedings of the 20th International Conference on Real-Time
 * and Network Systems (RTNS 2012), 2012, 61-70
 * 
 * Available at:
 * https://github.com/t-crest/s4noc/tree/master/noc/vhdl/generated
 */

package s4noc

import Const._
import scala.util.Random

case class Destination(dest: Int, pathLength: Int)

class Schedule(val n: Int) {

  val s = n match {
    case 2 => ScheduleTable.FourNodes
    case 3 => ScheduleTable.NineNodes
    case 4 => ScheduleTable.SixTeenNodes
    case 5 => ScheduleTable.TwentyFiveNodes
    case 6 => ScheduleTable.ThirtySixNodes
    case 7 => ScheduleTable.FourtyNineNodes
    case 8 => ScheduleTable.SixtyFourNodes
    case 9 => ScheduleTable.EightyOneNodes
    case 10 => ScheduleTable.OneHundredNodes
    case _ => throw new Error("Currently only 2x2 up to 10x10 NoCs supported, you requested: "+n+"x"+n)
  }

  def port(c: Char) = {
    c match {
      case 'n' => NORTH
      case 'e' => EAST
      case 's' => SOUTH
      case 'w' => WEST
      case 'l' => LOCAL
      case ' ' => 0
    }
  }

  def nextFrom(c: Char) = {
    c match {
      case 'n' => 's'
      case 'e' => 'w'
      case 's' => 'n'
      case 'w' => 'e'
      case 'l' => 'x' // no next for the last
      case ' ' => 'l' // stick to l on empty/waiting slots
    }
  }

  def nextFrom(i: Int) = {
    i match {
      case NORTH => SOUTH
      case EAST => WEST
      case SOUTH => NORTH
      case WEST => EAST
      case _ => throw new Error("wrong argument")
    }
  }

  // The array contains the MUX setting for each output port

  val split = s.split('|')
  val len = split.reduceLeft((a, b) => if (a.length > b.length) a else b).length
  val schedule = new Array[Array[Int]](len)
  val valid = new Array[Boolean](len)
  for (i <- 0 until len) {
    schedule(i) = Array.fill[Int](NR_OF_PORTS)(-1)
  }
  for (i <- 0 until split.length) {
    var from = 'l'
    for (j <- 0 until split(i).length) {
      val to = split(i)(j)
      if (to != ' ') {
        schedule(j)(port(to)) = port(from)
        from = nextFrom(to)
      }
    }
  }
  // valid is for the one-way memory
  var line = 0
  for (i <- 0 until len - 1) {
    // Need to think through this once more to check if correct
    if (line < split.length) {
      valid(i) = split(line)(i) != ' '
      if (valid(i)) line += 1
    }
  }
  println("Schedule for " + n * n + " nodes is " + schedule.length + " clock cycles")

  override def toString: String = {
    var s = ""
    schedule.foreach(a => {
      s += "( "
      a.foreach(v => {
        s += s"$v "
      })
      s += ")\n"
    })
    s
  }

  def move(core: Int, direction: Int): Int = {

    var row = core / n
    var col = core % n

    direction match {
      case NORTH => row = (row - 1 + n) % n
      case EAST => col = (col + 1) % n
      case SOUTH => row = (row + 1) % n
      case WEST => col = (col - 1 + n) % n
    }

    row * n + col
  }

  /**
    * Given a source core and a slot, it returns who the destination is
    * and how long it takes to reach it
    */
  def timeToDest(core: Int, slot : Int): Destination = {

    if (slot == -1) return Destination(-1, 0)
    var dest = core
    var step = schedule(slot).indexOf(LOCAL)
    var count = 0

    if (step == -1) return Destination(-1, 0)
    while (step != LOCAL) {
      dest = move(dest, step)
      count += 1
      step = schedule(slot + count).indexOf(nextFrom(step))
    }
    Destination(dest, count + 1)
  }

  /**
    * Return the time slot to be used from src to reach a dest
    */
  def coreToTimeSlot(src: Int, dest: Int) = {
    var time = -1
    for (i <- 0 until schedule.length) {
      if (timeToDest(src, i).dest == dest) {
        time = i
      }
    }
    time
  }

  /**
    * Giving a receive slot and the receiving core, it returns the sender (source).
    * @param core
    */
  def timeToSource(dest: Int, slot: Int) = {

    var source = -1
    for (sendingCore <- 0 until n * n) {
      for (sendingSlot <-0 until schedule.length) {
        val d = timeToDest(sendingCore, sendingSlot)
        if (d.dest == dest && slot == (sendingSlot + d.pathLength) % schedule.length) {
          source = sendingCore
        }
      }
    }
    source
  }
}


object Schedule {

  private var schedInst = new Schedule(2)

   def apply(n: Int) = {
     // for sbt parallel testing synchronize access to this singleton
     this.synchronized {
       if (schedInst.n != n) {
         schedInst = new Schedule(n)
       }
       schedInst
     }
  }

  /* A 2x2 schedule is as follows:
ne
  n
   e
 */

  def gen2x2Schedule() = {
    Array(Array(LOCAL, 0, 0, 0, 0), // P1: enter from local and exit to north register
      Array(0, SOUTH, 0, 0, 0), // P1: enter from south and exit to east register
      Array(LOCAL, 0, 0, 0, WEST), // P2: local to north, P1: from west to local
      Array(0, LOCAL, 0, 0, SOUTH), // P3: local to east, P2: south to local
      Array(0, 0, 0, 0, WEST)) // P3: from west to local
    // The last drain from west to local increases the schedule length by 1,
    // but could be overlapped.
    // Which means having it in the first slot, as there is no exit in the first slot.
  }

  def genRandomSchedule(slen: Int) = {
    val schedule = new Array[Array[Int]](slen)
    for (i <- 0 until slen) {
      val oneSlot = new Array[Int](NR_OF_PORTS)
      for (j <- 0 until NR_OF_PORTS) {
        oneSlot(j) = Random.nextInt(5)
      }
      schedule(i) = oneSlot
    }
    schedule
  }

  def main(args: Array[String]): Unit = {

    val s = Schedule(2)
    println(s)
    for (i <- 0 until s.schedule.length) {
      println(s.timeToDest(3,i))
    }
  }

}
