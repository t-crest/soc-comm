package arbiter

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// TODO: finish the test (and copy to the Chisel book -- maybe)
class ArbiterTest extends AnyFlatSpec with ChiselScalatestTester {

  def testBasic[T <: ArbiterTree[_ <: UInt]](dut: T) = {
    for (i <- 0 until 4) {
      dut.io.in(i).valid.poke(false.B)
    }
    dut.io.out.ready.poke(false.B) // Keep the output till we read it
    dut.io.in(2).valid.poke(true.B)
    dut.io.in(2).bits.poke(2.U)
    while(!dut.io.in(2).ready.peek().litToBoolean) {
      dut.clock.step()
    }
    dut.clock.step()
    dut.io.in(2).valid.poke(false.B)
    dut.clock.step(10)
    dut.io.out.bits.expect(2.U)
  }

  "ArbiterTree" should "pass" in {
    test(new ArbiterTree(4, UInt(8.W))) { dut =>
      testBasic(dut)
    }
  }

  "ArbiterTree" should "not loose inputs" in {
  }


  def testFair[T <: ArbiterTree[_ <: UInt]](dut: T) = {
    for (i <- 0 until 5) {
      dut.io.in(i).valid.poke(true.B)
      dut.io.in(i).bits.poke((i*100).U)
    }
    // println("Result should be " + List(0, 100, 200, 300, 400).sum)
    dut.io.out.ready.poke(true.B)
    dut.clock.step()
    for (i <- 0 until 40) {
      if (dut.io.out.valid.peek().litToBoolean) {
        println(dut.io.out.bits.peek().litValue)
      }
      dut.clock.step()
    }
  }

  "ArbiterTree" should "be fair" in {
    test(new ArbiterTree(5, UInt(16.W))) { dut => testFair(dut) }
  }
}


