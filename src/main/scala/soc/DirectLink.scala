/*
 * A direct link between cores. One register or a bubble FIFO (or a Chisel Queue).
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 *
 */

package soc

import Chisel._

// We should use DecoupledIO and the Queue from Chisel

class Interface extends Bundle {
  val ready = Output(Bool())
  val valid = Input(Bool())
  val data = Input(UInt(32.W))
}

class SimpleBuffer extends Module {
  val io = IO(new Bundle {
    val in = new Interface()
    val out = new Interface().flip()
  })

  val full = RegInit(false.B)
  val data = RegInit(0.U(32.W))

  io.in.ready := true.B
  io.out.valid := false.B

  when (full) {
    io.in.ready := false.B
    io.out.valid := true.B
    when (io.out.ready) {
      full := false.B
    }
  } .otherwise {
    when (io.in.valid) {
      data := io.in.data
      full := true.B
    }
  }

  io.out.data := data
}

class DirectLink(nrCores: Int) extends MultiCoreDevice(nrCores, 4*2) {

  // val connections = (0 until nrCores).map(i => (0 until 4).map(j => new Queue(new Decoupled(UInt(width=32), 1))
  val buffers = (0 until nrCores).map(i => (0 until 4).map(j => Module(new SimpleBuffer())))
  buffers(0)(2).io.in.data := 0.U

  io.ports(0).rdData := 42.U
}

object DirectLink extends App {

  // chiselMain(Array(), () => Module(new DirectLink(4)))
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new DirectLink(4))
}