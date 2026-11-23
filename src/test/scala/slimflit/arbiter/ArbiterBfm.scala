package slimflit.arbiter

import chisel3._
import chisel3.util._
import chiseltest._

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

