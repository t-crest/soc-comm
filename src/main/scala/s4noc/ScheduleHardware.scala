/*
 * Copyright: 2017, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * License: Simplified BSD License
 *
 * Get the size of a schedule table.
 *
 */

package s4noc

import chisel3._
import chisel3.util._

class ScheduleHardware[T <: Data](schedule: Array[Array[Int]], dt: T) extends Module {
  val io = IO(new Bundle() {
    val in = Input(UInt(16.W))
    val out = Output(Vec(Const.NR_OF_PORTS, UInt(3.W)))
  })

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

  val currentSched = sched(io.in)

  for (j <- 0 until Const.NR_OF_PORTS) {
    io.out(j) := currentSched(j)
  }
}

object ScheduleHardware extends App {
  emitVerilog(new ScheduleHardware(Schedule(args(0).toInt).schedule, UInt(32.W)), Array("--target-dir", "generated"))
}
