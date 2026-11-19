package slimflit.arbiter

import chisel3._
import chisel3.util.{isPow2, log2Ceil, ArbiterIO, OHToUInt}

object RotateArbiter extends ArbiterFactory {
  def name: String = "RotateArbiter"
  def firstComeFirstServe: Boolean = true
  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] } = new Arbiter(d, n, new RotateUnrotateMutex(n))
}

/// Weber, Matt. "Arbiters: design ideas and coding styles." SNUG Boston (2001).
class RotateUnrotateMutex(n: Int) extends Module {
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

  val release = VecInit(gnt.zip(io.rel).map { case (g, r) => g && r}).reduceTree(_ || _)

  val granted = gnt.reduceTree(_ || _)


  when(release) {
    counterReg := OHToUInt(gnt) + 1.U
  }.elsewhen(granted) {
    counterReg := OHToUInt(gnt)
  }

}