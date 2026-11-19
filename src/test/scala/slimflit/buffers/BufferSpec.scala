package slimflit.buffers

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec


class BufferBfm[T <: Data](io: QueueIO[T], val clock: Clock) {

  def enqueue(data: T): Long = {
    io.enq.bits.poke(data)
    io.enq.valid.poke(1.B)

    while (!io.enq.ready.peekBoolean()) {
      clock.step()
    }
    val t = clock.getStepCount
    
    clock.step()
    io.enq.valid.poke(0.B)
    t
  }

  def dequeue(): (T, Long) = {
    io.deq.ready.poke(true.B)

    while (!io.deq.valid.peekBoolean()) {
      clock.step()
    }

    val data = io.deq.bits.peek()
    val t = clock.getStepCount
    clock.step()
    io.deq.ready.poke(false.B)
    data -> t
  }

  def expectDequeue(expected: T): Long = {
    io.deq.ready.poke(true.B)

    while (!io.deq.valid.peekBoolean()) {
      clock.step()
    }

    io.deq.bits.expect(expected)
    val t = clock.getStepCount
    clock.step()
    io.deq.ready.poke(false.B)
    t
  }

}

abstract class BufferSpec(f: BufferFactory, minCapacity: Int, maxCapacity: Int, throughput: Double) extends AnyFlatSpec with ChiselScalatestTester {

  def rate(cycles: Seq[Long]): Double =
    if (cycles.length < 2) 1.0
    else (cycles.length - 1).toDouble / (cycles.last - cycles.head)

  def gaps(cycles: Seq[Long]): String =
    cycles.sliding(2).map(s => s(1) - s(0)).mkString(",")

  def queueTest(depth: Int, txs: Seq[UInt], delay: => Int, serial: Boolean, expectedThroughput: Option[Double]) = {
    test(f.buffer(UInt(8.W), depth))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      val bfm = new BufferBfm(dut.io, dut.clock)

      val enqCycles = scala.collection.mutable.ArrayBuffer[Long]()
      val deqCycles = scala.collection.mutable.ArrayBuffer[Long]()

      if (serial) {
        for (tx <- txs) {
          enqCycles += bfm.enqueue(tx)
          dut.clock.step(delay)
        }
        for (tx <- txs) {
          deqCycles += bfm.expectDequeue(tx)
          dut.clock.step(delay)
        }
      } else {
        fork {
          for (tx <- txs) {
            enqCycles += bfm.enqueue(tx)
            dut.clock.step(delay)
          }
        }.fork {
          for (tx <- txs) {
            deqCycles += bfm.expectDequeue(tx)
            dut.clock.step(delay)
          }
        }.joinAndStep(bfm.clock)
      }
      
      expectedThroughput.foreach { r =>
        val (enqRate, deqRate) = (rate(enqCycles.toSeq), rate(deqCycles.toSeq))
        assert(enqRate >= r, s"enq throughput $enqRate < $r, gaps: ${gaps(enqCycles.toSeq)}")
        assert(deqRate >= r, s"deq throughput $deqRate < $r, gaps: ${gaps(deqCycles.toSeq)}")
      }

    }
  }


  f.name should s"work at capacity=$minCapacity" in queueTest(
    depth = minCapacity,
    txs = Seq.range(0, 10).map(_.U), 
    delay = 0,
    serial = false,
    expectedThroughput = None
  )

  if (maxCapacity > minCapacity) it should s"work at capacity=$maxCapacity" in queueTest(
    depth = maxCapacity,
    txs = Seq.range(0, 10).map(_.U),
    delay = 0,
    serial = false,
    expectedThroughput = None
  )

  it should s"hold $maxCapacity items" in queueTest(
    depth = maxCapacity,
    txs = Seq.range(0, maxCapacity).map(_.U),
    delay = 0,
    serial = true,
    expectedThroughput = None
  )

  it should s"sustain throughput=$throughput" in queueTest(
    depth = 10.min(maxCapacity),
    txs = Seq.range(0, 20).map(_.U),
    delay = 0,
    serial = false,
    expectedThroughput = Some(throughput)
  )

}


class BubbleBufferSpec extends BufferSpec(BubbleBuffer, 1, 64, 0.5)
class SkidBufferSpec extends BufferSpec(SkidBuffer, 2, 2, 1.0)
class GenericQueueSpec extends BufferSpec(GenericQueue, 1, 128, 1.0)