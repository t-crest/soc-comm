package slimflit.buffers

import chisel3._
import chisel3.util._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec

class BubbleBufferSpec extends BufferSpec(
  BubbleBuffer, 
  minCapacity = 1, 
  maxCapacity = 64, 
  throughput = 0.5
)
class SkidBufferSpec extends BufferSpec(
  SkidBuffer, 
  minCapacity = 2, 
  maxCapacity = 2, 
  throughput = 1.0
)
class GenericQueueSpec extends BufferSpec(
  GenericQueue, 
  minCapacity = 1, 
  maxCapacity = 128, 
  throughput = 1.0
)

abstract class BufferSpec(f: BufferFactory, minCapacity: Int, maxCapacity: Int, throughput: Double) extends AnyFlatSpec with ChiselScalatestTester {

  val annotations: firrtl.AnnotationSeq = Seq()

  def rate(cycles: Seq[Long]): Double =
    if (cycles.length < 2) 1.0
    else (cycles.length - 1).toDouble / (cycles.last - cycles.head)

  def gaps(cycles: Seq[Long]): String =
    cycles.sliding(2).map(s => s(1) - s(0)).mkString(",")

  def queueTest(depth: Int, txs: Seq[UInt], delay: => Int, serial: Boolean, expectedThroughput: Option[Double]) = {
    test(f.buffer(UInt(8.W), depth))
      .withAnnotations(annotations) { dut =>
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


