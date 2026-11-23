package slimflit.arbiter

import chisel3._
import chisel3.util.{isPow2, log2Ceil, ArbiterIO, OHToUInt}

case class RotateArbiter(firstComeFirstServe: Boolean) extends ArbiterFactory {
  def name: String = "RotateArbiter"

  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] } = new Arbiter(d, n, new RotateUnrotateMutex(n, firstComeFirstServe))
}

/// Weber, Matt. "Arbiters: design ideas and coding styles." SNUG Boston (2001).
class RotateUnrotateMutex(n: Int, firstComeFirstServe: Boolean) extends Module {
  require(isPow2(n), "RotateUnrotateMutex requires n to be a power of 2")

  val io = IO(new MutexIO(n))

  val counterReg = RegInit(0.U(log2Ceil(n).W))

  val rotated = BarrelRotater.left(io.req, counterReg)

  val (rotatedGnt, _) = rotated.foldLeft(Seq.empty[Bool] -> 0.B) { case ((grants, alreadyTaken), req) =>
    val take = req && !alreadyTaken
    (grants :+ take, alreadyTaken || take)
  }

  val gnt = BarrelRotater.right(rotatedGnt, counterReg)
  io.gnt := gnt

  val granted = gnt.reduceTree(_ || _)

  val chosen = OHToUInt(gnt)
  io.chosen := chosen


  if (firstComeFirstServe) {
    when(io.release) {
      counterReg := chosen + 1.U
    }.elsewhen(granted) {
      counterReg := chosen
    }
  } else {
    when(io.release) {
      counterReg := chosen + 1.U
    }
  }

}