// scala
package slimflit

import chisel3._
import chisel3.util._

class Router(nodeX: Int, nodeY: Int) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Vec(5, Decoupled(new Packet))) // 0:local,1:N,2:E,3:S,4:W (as sources)
    val out = Vec(5, Decoupled(new Packet))           // 0:local,1:N,2:E,3:S,4:W (as destinations)
  })

  // routing function (XY): decide output port for a packet
  def routePort(p: Packet): UInt = {
    val destX = p.x
    val destY = p.y
    val nx = nodeX.S
    val ny = nodeY.S

    val ret = WireDefault(0.U)  // default

    // If destination equals this node -> local
    when(destX === nx && destY === ny) {
      ret := 0.U
    } .elsewhen(destX =/= nx) {
      // route X first: east or west
      when(destX > nx) { ret := 2.U } .otherwise { ret := 4.U }
    } .otherwise {
      // X equal, route in Y
      when(destY > ny) { ret := 1.U } .otherwise { ret := 3.U }
    }
    ret
  }

  // Precompute destination port for each input and valid flags
  val dest = Wire(Vec(5, UInt(3.W)))
  val inValids = Wire(Vec(5, Bool()))
  val inBits = Wire(Vec(5, new Packet))
  for (i <- 0 until 5) {
    dest(i) := routePort(io.in(i).bits)
    inValids(i) := io.in(i).valid
    inBits(i) := io.in(i).bits
  }

  // For each output, pick highest-priority input that targets it
  val selectedIdx = Wire(Vec(5, UInt(log2Ceil(5).W)))
  val anyMatch = Wire(Vec(5, Bool()))
  for (o <- 0 until 5) {
    val matches = VecInit((0 until 5).map(i => inValids(i) && (dest(i) === o.U)))
    anyMatch(o) := matches.asUInt.orR
    selectedIdx(o) := Mux(anyMatch(o), PriorityEncoder(matches), 0.U)
    io.out(o).valid := anyMatch(o)
    io.out(o).bits := inBits(selectedIdx(o))
  }

  // Input ready when it is selected for its destination and that output is ready
  for (i <- 0 until 5) {
    val outPort = dest(i)
    // compare selectedIdx(outPort) == i.U; when anyMatch(outPort) is false, selectedIdx is 0 but valid is false so ready should be false
    val selectedForOut = Wire(Bool())
    selectedForOut := false.B
    // compute equality by checking each possible output
    for (o <- 0 until 5) {
      when(outPort === o.U) {
        selectedForOut := anyMatch(o) && (selectedIdx(o) === i.U)
      }
    }
    io.in(i).ready := selectedForOut && io.out(outPort.litValue.toInt).ready
  }

  // Default: tie off outputs when not valid (already handled), nothing else required
}
