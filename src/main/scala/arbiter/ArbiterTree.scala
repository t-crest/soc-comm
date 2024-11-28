package arbiter

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum


// Only one will be ready, as we cannot take two values
// This would need a shadow register, a reasonable optimisation
// Without optimisation one channel can only take one data every 2 clock cycles
class ArbiterTree[T <: Data: Manifest](n: Int, private val gen: T) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(n, new DecoupledIO(gen)))
    val out = new DecoupledIO(gen)
  })
  def arbitrateFair(a: DecoupledIO[T], b: DecoupledIO[T]) = {
    object State extends ChiselEnum {
      val idleA, idleB, hasA, hasB = Value
    }
    import State._
    val regData = Reg(gen)
    val regState = RegInit(idleA)
    val out = Wire(new DecoupledIO(gen))
    a.ready := regState === idleA
    b.ready := regState === idleB
    out.valid := (regState === hasA || regState === hasB)
    switch(regState) {
      is (idleA) {
        when (a.valid) {
          regData := a.bits
          regState := hasA
        } otherwise {
          regState := idleB
        }
      }
      is (idleB) {
        when (b.valid) {
          regData := b.bits
          regState := hasB
        } otherwise {
          regState := idleA
        }
      }
      is (hasA) {
        when (out.ready) {
          regState := idleB
        }
      }
      is (hasB) {
        when (out.ready) {
          regState := idleA
        }
      }
    }
    out.bits := regData
    out
  }

  io.out <> io.in.reduceTree((a, b) => arbitrateFair(a, b))
}
