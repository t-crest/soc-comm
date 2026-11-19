package slimflit.arbiter

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

class BarrelRotaterSpec extends AnyFlatSpec with ChiselScalatestTester {
  
  class Rotater[T <: Data](n: Int, d: T) extends Module {
    val in = IO(Input(Vec(n, d)))
    val shamt = IO(Input(UInt(log2Ceil(n).W)))
    val outLeft = IO(Output(Vec(n, d)))
    val outRight = IO(Output(Vec(n, d)))

    outLeft := BarrelRotater.left(in, shamt)
    outRight := BarrelRotater.right(in, shamt)
  }

  def rotateLeft[T](seq: Seq[T], n: Int): Seq[T] =
    if (seq.isEmpty) seq
    else {
      val k = Math.floorMod(n, seq.length)
      seq.drop(k) ++ seq.take(k)
    }

  def rotateRight[T](seq: Seq[T], n: Int): Seq[T] = rotateLeft(seq, -n)

  "BarrelRotater" should "rotate left" in {
    val n = 128
    val w = log2Ceil(n+1).W
    test(new Rotater(n, UInt(w))) { dut =>
      for (i <- 0 until n) {
        dut.in(i).poke((i + 1).U)
      }
      for (shamt <- 0 until n) {
        dut.shamt.poke(shamt.U)
        dut.clock.step()
        val expected = rotateLeft(Seq.range(1, n + 1), shamt)
        dut.outLeft.zip(expected).foreach { case (out, exp) =>
          out.expect(exp.U)
        }
      }  
    }
  }

  it should "rotate right" in {
    val n = 128
    val w = log2Ceil(n+1).W
    test(new Rotater(n, UInt(w))) { dut =>
      for (i <- 0 until n) {
        dut.in(i).poke((i + 1).U)
      }
      for (shamt <- 0 until n) {
        dut.shamt.poke(shamt.U)
        dut.clock.step()
        val expected = rotateRight(Seq.range(1, n + 1), shamt)
        dut.outRight.zip(expected).foreach { case (out, exp) =>
          out.expect(exp.U)
        }
      }  
    }
  }

}