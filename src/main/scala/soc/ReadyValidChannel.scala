package soc

import chisel3.util.DecoupledIO
import chisel3.{Bundle, Data, Flipped}
import s4noc.Entry

// TODO: get rid of Entry here
class ReadyValidChannel[T <: Data](private val dt: T) extends Bundle {
  val tx = Flipped(new DecoupledIO(Entry(dt)))
  val rx = new DecoupledIO(Entry(dt))
}
