package slimflit.buffers

import chisel3._
import chisel3.util._
import chiseltest._

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
