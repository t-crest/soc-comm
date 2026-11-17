package slimflit.arbiter

import chisel3._
import chisel3.util._

class RotateUnrotateMutex(n: Int) extends Module {
  require(isPow2(n), "RotateUnrotateMutex requires n to be a power of 2")

  val io = IO(new MutexIO(n))

  val counterReg = RegInit(0.U(log2Ceil(n).W))

  val rotated = BarrelRotater.left(io.req, counterReg)

  val rotatedGnt = rotated.foldLeft(Seq.empty[Bool] -> 0.B) { case ((grants, alreadyTaken), req) =>
    val take = req && !alreadyTaken
    (grants :+ take, alreadyTaken || take)
  }._1

  val gnt = BarrelRotater.right(rotatedGnt, counterReg)
  io.gnt := gnt

  val release = VecInit(gnt.zip(io.rel).map { case (g, r) => g && r}).reduceTree(_ || _)

  when(release) {
    counterReg := counterReg + 1.U
  }

}