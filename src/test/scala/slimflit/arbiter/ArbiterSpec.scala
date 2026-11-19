package slimflit.arbiter

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec
import slimflit.buffers.SkidBuffer

class ArbiterBfm[T <: Data](io: ArbiterIO[T], val clock: Clock) {

  def enqueue(i: Int, data: T): Long = {
    io.in(i).bits.poke(data)
    io.in(i).valid.poke(1.B)

    var t = 0L

    fork.withRegion(Monitor) {
      while (!io.in(i).ready.peekBoolean()) {
        clock.step()
      }
      t = clock.getStepCount
    }.joinAndStep(clock)
  
    io.in(i).valid.poke(0.B)
    t
  }

  def dequeue(): (T, Long) = {
    io.out.ready.poke(true.B)

    var data = null.asInstanceOf[T]
    var t = 0L

    fork.withRegion(Monitor) {
      while (!io.out.valid.peekBoolean()) {
        clock.step()
      }
      data = io.out.bits.peek()
      t = clock.getStepCount
    }.joinAndStep(clock)
    
    io.out.ready.poke(false.B)
    data -> t
  }

  def expectDequeue(expected: T): Long = {
    io.out.ready.poke(true.B)

    var t = 0L

    fork.withRegion(Monitor) {
      while (!io.out.valid.peekBoolean()) {
        clock.step()
      }
      io.out.bits.expect(expected)
      t = clock.getStepCount
    }.joinAndStep(clock)

    io.out.ready.poke(false.B)
    t
  }

}

class ArbiterWrapped[T <: Data](f: ArbiterFactory, d: T, n: Int) extends Module {
  val io = IO(new ArbiterIO(d, n))
  val arb = Module(f.arbiter(d, n))
  val buf = Module(SkidBuffer.buffer(d, 2))
  io.in <> arb.io.in
  arb.io.out <> buf.io.enq
  buf.io.deq <> io.out
  io.chosen := arb.io.chosen
}

abstract class ArbiterSpec(f: ArbiterFactory) extends AnyFlatSpec with ChiselScalatestTester {


  f.name should "serve requests after at most n-1 cycles" in {
    for (n <- Seq(2, 4, 8, 16, 32)) {

      val w = 8.W
      val reps = 10
      test(new ArbiterWrapped(f, UInt(w), n)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val bfm = new ArbiterBfm(dut.io, dut.clock)


        val forks = for (i <- 0 until n) yield fork {
          for (j <- 0 until reps) {
            val start = dut.clock.getStepCount
            val end = bfm.enqueue(i, (i + 1).U)
            assert(end - start <= (n - 1), s"Request $i was served in ${end - start} cycles, expected no more than ${n - 1} cycles")
          }
        }

        for (i <- 0 until (n * reps)) {
          bfm.dequeue()
        }
        forks.foreach(_.join())
      }
    }
  }

  it should "alternate fairly between any two requests" in {
    for (n <- Seq(2, 4, 8, 16)) {
      val w = 8.W
      val reps = 8
      test(new ArbiterWrapped(f, UInt(w), n)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val bfm = new ArbiterBfm(dut.io, dut.clock)


        // all combinations of two requests
        for (a <- Seq.range(0, n); b <- Seq.range(0, n) if a != b) {

          val forks = for (i <- Seq(a, b)) yield fork {
            for (j <- 0 until reps) {
              val start = dut.clock.getStepCount
              val end = bfm.enqueue(i, i.U)
              assert(end - start <= 1, s"Request $i was served in ${end - start} cycles, expected no more than 1 cycles")
            }
          }

          for (k <- 0 until reps) {
            val (data, _) = bfm.dequeue()
            if (data.litValue == a) {
              bfm.expectDequeue(b.U)
            } else {
              bfm.expectDequeue(a.U)
            }
          }

          forks.foreach(_.join())
        
          dut.clock.step(10) 
        }
      }
    }
  }

  if (f.firstComeFirstServe) it should "hold a grant under backpressure" in {
    // plan: let requests arrive staggered, and first dequeue after all requests have arrived
    // the first request to arrive should be granted first
    val n = 4
    val w = 8.W
    test(new ArbiterWrapped(f, UInt(w), n)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      
      val bfm = new ArbiterBfm(dut.io, dut.clock)


      // insert two requests to fill the buffer
      val t0 = bfm.enqueue(0, 0.U)
      dut.clock.step()
      val t1 = bfm.enqueue(0, 1.U)
      dut.clock.step(1)

      val ts = fork {
        // 3 comes first
        val t3 = bfm.enqueue(3, 3.U)
      }.fork {
        // 2 comes second
        dut.clock.step()
        val t2 = bfm.enqueue(2, 2.U)        
      }.fork {
        dut.clock.step(2)
        val t0 = bfm.enqueue(0, 0.U)
      }.fork {
        // 1 comes last
        dut.clock.step(3)
        val t1 = bfm.enqueue(1, 1.U)
      }

      dut.clock.step(5)
      val _ = bfm.dequeue()
      dut.clock.step(1)
      val _ = bfm.dequeue()
      dut.clock.step(1)
      val (data, _) = bfm.dequeue()
      assert(data.litValue == 3, s"Expected 3 to be granted first, but got ${data.litValue}")
      dut.clock.step(1)

      for (i <- 0 until 3) bfm.dequeue() 

      ts.joinAndStep(dut.clock)
      
    }
  }

  it should "provide a single requester with full throughput" in {
    // plan: for each input, let it send a burst of 10 and check that there is full throughput at the ingress
    // egress is eagerly consumed
  }


}

class ChiselRRArbiterSpec extends ArbiterSpec(ChiselRRArbiter)
class RotateArbiterSpec extends ArbiterSpec(RotateArbiter)
class MaskedPrioArbiterSpec extends ArbiterSpec(MaskedPrioArbiter)