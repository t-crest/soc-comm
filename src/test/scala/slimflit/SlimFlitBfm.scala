package slimflit

import chisel3._
import chisel3.util._
import chiseltest._

class SlimFlitBfm[T <: Data](sf: SlimFlit[T]) {

  implicit val p: SlimFlitParams[T] = sf.params

  def enqueue(coord: Coord)(p: SlimPack[T]): Long = {
    enqueue(sf.ports(coord).ingress, p)
  }

  def enqueueSeq(coord: Coord)(ps: Seq[SlimPack[T]]): Long = {
    enqueueSeq(sf.ports(coord).ingress, ps)
  }

  def dequeue(coord: Coord): (SlimPack[T], Long) = {
    dequeue(sf.ports(coord).egress)
  }

  def dequeueSeq(coord: Coord, n: Int): (Seq[SlimPack[T]], Long) = {
    dequeueSeq(sf.ports(coord).egress, n)
  }

  def dequeueExpect(coord: Coord)(expected: SlimPack[T]): Long = {
    dequeueExpect(sf.ports(coord).egress, expected)
  }

  def dequeueExpectSeq(coord: Coord)(expectedSeq: Seq[SlimPack[T]]): Long = {
    dequeueExpectSeq(sf.ports(coord).egress, expectedSeq)
  }

  private def enqueue[D <: Data](port: DecoupledIO[D], d: D): Long = {
    port.bits.poke(d)
    port.valid.poke(1.B)

    var t = 0L

    fork.withRegion(Monitor) {
      while (!port.ready.peekBoolean()) {
        sf.clock.step()
      }
      t = sf.clock.getStepCount
    }.joinAndStep(sf.clock)

    port.valid.poke(0.B)
    t
  }

  private def enqueueSeq[D <: Data](port: DecoupledIO[D], dataSeq: Seq[D]): Long = {
    var t = 0L
    for (d <- dataSeq) {
      t += enqueue(port, d)
    }
    t
  }

  private def dequeue[D <: Data](port: DecoupledIO[D]): (D, Long) = {
    port.ready.poke(true.B)

    var data = null.asInstanceOf[D]
    var t = 0L

    fork.withRegion(Monitor) {
      while (!port.valid.peekBoolean()) {
        sf.clock.step()
      }
      data = port.bits.peek()
      t = sf.clock.getStepCount
    }.joinAndStep(sf.clock)

    port.ready.poke(false.B)
    data -> t
  }
  
  private def dequeueSeq[D <: Data](port: DecoupledIO[D], n: Int): (Seq[D], Long) = {
    var dataSeq = Seq.empty[D]
    var t = 0L
    for (_ <- 0 until n) {
      val (data, dt) = dequeue(port)
      dataSeq :+= data
      t += dt
    }
    dataSeq -> t
  }

  private def dequeueExpect[D <: Data](port: DecoupledIO[D], expected: D): Long = {
    port.ready.poke(true.B)

    var t = 0L

    fork.withRegion(Monitor) {
      while (!port.valid.peekBoolean()) {
        sf.clock.step()
      }
      port.bits.expect(expected)
      t = sf.clock.getStepCount
    }.joinAndStep(sf.clock)

    port.ready.poke(false.B)
    t
  }

  private def dequeueExpectSeq[D <: Data](port: DecoupledIO[D], expectedSeq: Seq[D]): Long = {
    var t = 0L
    for (expected <- expectedSeq) {
      t += dequeueExpect(port, expected)
    }
    t
  }

  

}
