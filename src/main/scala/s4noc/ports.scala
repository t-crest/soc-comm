package s4noc

import chisel3._


/**
 * Channel directions
 */
object Const {
  val NORTH = 0
  val EAST = 1
  val SOUTH = 2
  val WEST = 3
  val LOCAL = 4
  val INVALID = 5
  val NR_OF_PORTS = 5
}

class SingleChannelIO[T <: Data](private val dt: T) extends Bundle {
  val data = dt.cloneType
  val valid = Bool()
}

class ChannelIO[T <: Data](private val dt: T) extends Bundle {
  val out = Output(new SingleChannelIO(dt))
  val in = Input(new SingleChannelIO(dt))
}

class RouterIO[T <: Data](private val dt: T) extends Bundle {
  val ports = Vec(Const.NR_OF_PORTS, new ChannelIO(dt))
}

// This should be a generic data for the FIFO
class Entry[T <: Data](private val dt: T) extends Bundle {
  val data = dt.cloneType
  val core = UInt(8.W)
}

object Entry {
  def apply[T <: Data](dt: T) = {
    new Entry(dt)
  }
}

// TODO: why comb?
class CpuPortCombIO(private val w: Int) extends Bundle {
  val addr = Input(UInt(8.W))
  val rdData = Output(UInt(w.W))
  val wrData = Input(UInt(w.W))
  val rd = Input(Bool())
  val wr = Input(Bool())
}
