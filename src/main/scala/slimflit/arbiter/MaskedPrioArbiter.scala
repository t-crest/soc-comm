package slimflit.arbiter

import chisel3._
import chisel3.util.{isPow2, log2Ceil, ArbiterIO, OHToUInt}

case class MaskedPrioArbiter(firstComeFirstServe: Boolean) extends ArbiterFactory {
  def name: String = "MaskedPrioArbiter"
  def arbiter[T <: Data](d: T, n: Int): Module { val io: ArbiterIO[T] } = new Arbiter(d, n, new MaskedPrioMutex(n, firstComeFirstServe))
}

/// Weber, Matt. "Arbiters: design ideas and coding styles." SNUG Boston (2001).
class MaskedPrioMutex(n: Int, firstComeFirstServe: Boolean) extends Module {
  require(isPow2(n), "MaskedPrioMutex requires n to be a power of 2")
  val io = IO(new MutexIO(n))

  val counterReg = RegInit(0.U(log2Ceil(n).W))

  val maskLookup = VecInit((0 until n).map { i =>
    val base = (BigInt(1) << n) - 1
    val zeroMask = (BigInt(1) << i) - 1
    val mask = base - zeroMask
    mask.U
  })
  val mask = maskLookup(counterReg)

  val (unmaskedGnt, _) = io.req.foldLeft(Seq.empty[Bool] -> 0.B) { case ((grants, alreadyTaken), req) =>
    val take = req && !alreadyTaken
    (grants :+ take, alreadyTaken || take)
  }

  val maskedReq = io.req.zip(mask.asBools).map { case (r, m) => r && m }

  val noMaskNeeded = !VecInit(maskedReq).reduceTree(_ || _)

  val (maskedGnt, _) = maskedReq.foldLeft(Seq.empty[Bool] -> 0.B) { case ((grants, alreadyTaken), req) =>
    val take = req && !alreadyTaken
    (grants :+ take, alreadyTaken || take)
  }

  val gnt = maskedGnt.zip(unmaskedGnt).map { case (mg, ug) => mg || (ug && noMaskNeeded) }

  io.gnt := gnt

  val granted = VecInit(gnt).reduceTree(_ || _)

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
