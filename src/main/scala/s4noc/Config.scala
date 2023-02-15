package s4noc

import chisel3._
import chisel.lib.fifo._
class FifoType(val depth: Int) {
  def getFifo[T <: Data](dt: T) = {
    this match {
      case MemType(depth) => Module(new MemFifo(Entry(dt), depth))
      case BubbleType(depth) => Module(new BubbleFifo(Entry(dt), depth))
      case DoubleBubbleType(depth) => Module(new DoubleBufferFifo(Entry(dt), depth))
      case RegType(depth) => Module(new RegFifo(Entry(dt), depth))
    }
  }
}
case class MemType(override val depth: Int) extends FifoType(depth)
case class BubbleType(override val depth: Int) extends FifoType(depth)
case class DoubleBubbleType(override val depth: Int) extends FifoType(depth)
case class RegType(override val depth: Int) extends FifoType(depth)

/**
  * S4NoC Configuration case class
  * @param n nodes
  * @param txDepth tx FIFO depth
  * @param rxDepth rx FIFO depth
  * @param width channel width (usually 32)
  */
case class Config(n: Int, tx: FifoType, split: FifoType, rx: FifoType, width: Int) {
  val dim = math.sqrt(n).toInt
  if (dim * dim != n) throw new Error("Number of cores must be quadratic")


}

object x extends App {

  def foo(b: FifoType) = {
    println(b.depth)
    b match {
      case MemType(depth) => println(s"Mem ${b.depth}")
      case BubbleType(depth) => println(s"Bubble ${b.depth}")
    }
  }
  val t = BubbleType(10)
  println(t.depth)
  foo(t)

  val t2 = MemType(13)
  foo(t2)

}
