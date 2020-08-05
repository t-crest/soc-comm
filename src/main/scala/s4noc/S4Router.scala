/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 * 
 * A router for the S4NOC.
 * 
 */

package s4noc

import chisel3._
import chisel3.util._

/**
 * Channel directions
 */
object Const {
  val NORTH = 0
  val EAST = 1
  val SOUTH = 2
  val WEST = 3
  val LOCAL = 4
  val NR_OF_PORTS = 5
}

class SingleChannel[T <: Data](private val dt: T) extends Bundle {
  val data = dt.cloneType
  val valid = Bool()
}

class Channel[T <: Data](private val dt: T) extends Bundle {
  val out = Output(new SingleChannel(dt))
  val in = Input(new SingleChannel(dt))
}

class RouterPorts[T <: Data](private val dt: T) extends Bundle {
  val ports = Vec(Const.NR_OF_PORTS, new Channel(dt))
}

class S4Router[T <: Data](schedule: Array[Array[Int]], dt: T) extends Module {
  val io = IO(new RouterPorts(dt))

  val regCounter = RegInit(0.U(log2Up(schedule.length).W))
  val end = regCounter === (schedule.length - 1).U
  regCounter := Mux(end, 0.U, regCounter + 1.U)


  // Just convert schedule table to a Chisel type table
  // unused slot is -1, convert to 0.U
  val sched = Wire(Vec(schedule.length, Vec(Const.NR_OF_PORTS, UInt(3.W))))
  for (i <- 0 until schedule.length) {
    for (j <- 0 until Const.NR_OF_PORTS) {
      val s = schedule(i)(j)
      val v = if (s == -1) 0 else s
      sched(i)(j) := v.U(3.W)
    }
  }

  // TDM schedule starts one cycles later for read data delay
  val regDelay = RegNext(regCounter, init = 0.U)
  val currentSched = sched(regDelay)
  // TODO: test if this movement of the register past the schedule table works, better here a register
  // val currentSched = RegNext(sched(regCounter))

  val resetVal = Wire(new SingleChannel(dt))
  resetVal.data := 0.U
  resetVal.valid := false.B

  for (j <- 0 until Const.NR_OF_PORTS) {
    io.ports(j).out := RegNext(io.ports(currentSched(j)).in, init = resetVal)
  }
}

object S4Router extends App {

  chisel3.Driver.execute(Array("--target-dir", "generated"),
    () => new S4Router(Schedule.genRandomSchedule(7), UInt(32.W)))
}
