package slimflit

import chisel3._
import chisel3.util._
import chiseltest._

import slimflit.buffers._
import slimflit.arbiter._

import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class SlimFlitConnectionCheck extends AnyFlatSpec with ChiselScalatestTester {

  val annotations: firrtl.AnnotationSeq = Seq(VerilatorBackendAnnotation)

  // run tests for various network dimensions
  val tests = Seq(

    // test different network dimensions
    ((2, 2), GenericQueue, ChiselRRArbiter),
    ((3, 3), GenericQueue, ChiselRRArbiter),
    ((4, 4) , GenericQueue, ChiselRRArbiter),
    ((2, 8), GenericQueue, ChiselRRArbiter),
    ((8, 2), GenericQueue, ChiselRRArbiter),
    ((8, 8), GenericQueue, ChiselRRArbiter),

    // test buffers
    ((4,4), SkidBuffer, ChiselRRArbiter),
    ((4,4), BubbleBuffer, ChiselRRArbiter),

    // test arbiters
    ((4,4), GenericQueue, RotateArbiter(firstComeFirstServe = true)),
    ((4,4), GenericQueue, RotateArbiter(firstComeFirstServe = false)),
    ((4,4), GenericQueue, MaskedPrioArbiter(firstComeFirstServe = true)),
    ((4,4), GenericQueue, MaskedPrioArbiter(firstComeFirstServe = false))
  )
  
  for ((dim, buffer, arbiter) <- tests) {
    testConnections(dim, buffer, arbiter)
  }
  

  class Id {
    // tests threads don't run in parallel, 
    // so we can use a simple counter for unique IDs
    var id: Int = 0
    def next(): Int = {
      val current = id
      id += 1
      current
    }
  }

  def testConnections(dim: (Int, Int), buffer: BufferFactory, arbiter: ArbiterFactory): Unit = {

    implicit val config: SlimFlitParams[UInt] = SlimFlitParams[UInt](
      dim,
      payloadGen = UInt(32.W),
      bufferFactory = buffer,
      bufferDepth = 2,
      arbiterFactory = arbiter,
      topology = Mesh,
      debug = true
    )

    s"SlimFlit ${dim._1}x${dim._2} (${buffer.name}, ${arbiter.name})" should "send packets all-to-all serially" in {
      test(new SlimFlit(config)).withAnnotations(annotations) { dut =>

        val bfm = new SlimFlitBfm(dut)

        val packetIdGen = new Id()

        dut.clock.setTimeout(100)
        
        for (src <- config.coords; dst <- config.coords) if (src != dst) {
          val payload = packetIdGen.next()
          val packet = config.createPacket(src -> dst, payload.U)

          fork {
            bfm.enqueue(src)(packet)
          }.fork {
            bfm.dequeueExpect(dst)(packet)
          }.join()
        }

      }

    }

    it should "send packets all-to-all concurrently" in {
      test(new SlimFlit(config)).withAnnotations(annotations) { dut =>

        val bfm = new SlimFlitBfm(dut)

        val packetIdGen = new Id()

        val packets = for (src <- config.coords; dst <- config.coords if src != dst) yield {
          val payload = packetIdGen.next()
          config.createPacket(src -> dst, payload.U)
        }

        val producers = for (src <- config.coords) yield fork {
          for (packet <- packets if packet.srcCoord == src) {
            bfm.enqueue(src)(packet)
          }
        }

        val consumers = for (dst <- config.coords) yield fork {
          // make mutable map of packets to expect and pop from it as we receive packets
          val expectedPackets = mutable.Map(packets.filter(_.destCoord == dst).map(p => p.payload.litValue -> p).toSeq: _*)
          for (_ <- 0 until expectedPackets.size) {
            val (packet, _) = bfm.dequeue(dst)
            // remove packet from expectedPackets based on payload
            expectedPackets.remove(packet.payload.litValue).getOrElse(
    throw new Exception(s"Received unexpected packet with payload ${packet.payload.litValue} at $dst")
            )
          }
          assert(expectedPackets.isEmpty, s"Not all packets were received at $dst (${expectedPackets.size} remaining)")
        }

        producers.foreach(_.join())
        consumers.foreach(_.join())
      }
    }

  }



}
