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


class S4Router[T <: Data](schedule: Array[Array[Int]], dt: T) extends Module {
  val io = IO(new RouterIO(dt))

  val regCounter = RegInit(0.U(log2Up(schedule.length).W))
  val end = regCounter === (schedule.length - 1).U
  regCounter := Mux(end, 0.U, regCounter + 1.U)


  // Just convert schedule table to a Chisel type table
  // unused slot is -1, convert to 0.U
  val sched = Wire(Vec(schedule.length, Vec(Const.NR_OF_PORTS, UInt(3.W))))
  for (i <- 0 until schedule.length) {
    for (j <- 0 until Const.NR_OF_PORTS) {
      val s = schedule(i)(j)
      val v = if (s == -1) Const.INVALID else s
      sched(i)(j) := v.U(3.W)
    }
  }

  // TDM schedule starts one cycles later for read data delay
  val regDelay = RegNext(regCounter, init = 0.U)
  val currentSched = sched(regDelay)
  // TODO: test if this movement of the register past the schedule table works, better here a register
  // val currentSched = RegNext(sched(regCounter))

  for (j <- 0 until Const.NR_OF_PORTS) {
    io.ports(j).out.data := RegNext(io.ports(currentSched(j)).in.data)
    io.ports(j).out.valid := RegNext(Mux(currentSched(j) === Const.INVALID.U, false.B, io.ports(currentSched(j)).in.valid), init = false.B)
  }
}

object S4Router extends App {
  emitVerilog(new S4Router(Schedule.genRandomSchedule(7), UInt(32.W)), Array("--target-dir", "generated"))
}
