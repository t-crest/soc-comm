package slimflit.arbiter

import chisel3._
import chisel3.util._
import slimflit._

trait ArbiterFactory { 
  def name: String

  def create[T <: Data](egressDir: Direction, ingresses: Seq[Direction])(implicit p: SlimFlitParams[T]): PacketArbiter[T] = {
    new PacketArbiter(
      arbiter(new SlimPack[T](), ingresses.length),
      ingresses,
      egressDir
    )
  }

  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] }
  def firstComeFirstServe: Boolean
}

class PacketArbiter[T <: Data](
  arb: => Module { val io: ArbiterIO[SlimPack[T]] },
  from: Seq[Direction],
  to: Direction
)(implicit p: SlimFlitParams[T]) extends Module {
  override val desiredName: String = s"PacketArbiter_${to}"

  val in = IO(MapBundle(from, (d: Direction) => Flipped(new SlimPackPort[T](d))))
  val out = IO(new SlimPackPort[T](to))

  val arbiter = Module(arb)

  require(in.values.length == arbiter.io.in.length)
  arbiter.io.in.zip(in.values).foreach { case (arbIn, inPort) =>
    arbIn <> inPort
  }
  out <> arbiter.io.out

}

class Arbiter[T <: Data](d: T, n: Int, mutex: => Module { val io: MutexIO }) extends Module {
  val io = IO(new ArbiterIO(d, n))

  val m = Module(mutex)

  m.io.req := VecInit(io.in.map(_.valid))

  io.chosen := m.io.chosen

  m.io.release := io.out.ready

  val masked = io.in.zip(m.io.gnt).map { case (in, gnt) => in.bits.asUInt & Fill(d.getWidth, gnt) }

  io.out.bits := VecInit(masked).reduceTree(_ | _).asTypeOf(d)
  io.out.valid := VecInit(io.in.map(_.valid)).reduceTree(_ || _)

  io.in.zip(m.io.gnt).foreach { case (in, gnt) =>
    in.ready := gnt && io.out.ready
  }

}

object ChiselRRArbiter extends ArbiterFactory {
  def name: String = "ChiselRRArbiter"
  def firstComeFirstServe: Boolean = false
  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] } = new chisel3.util.RRArbiter(d, n)
}



class MutexIO(n: Int) extends Bundle {
  val req = Input(Vec(n, Bool()))
  val gnt = Output(Vec(n, Bool()))
  val release = Input(Bool())
  val chosen = Output(UInt(log2Ceil(n).W))
}



