package slimflit

import chisel3._
import chisel3.util._

class RoutingBlock[T <: Data](val ingressDir: Direction, coord: Coord)(
    implicit p: SlimFlitParams[T]
) extends Module {
  override val desiredName: String = s"RoutingBlock_${ingressDir}_${coord}"
  val in = IO(Flipped(new SlimPackPort[T](ingressDir)))
  
  // generate routing logic map
  val routes = p.routingPolicy
    .getRule(ingressDir)
    .generateLogic(in.bits.dest, coord)

  // create output ports for routing directions
  val out = IO(MapBundle(routes.keys.toSeq, new SlimPackPort[T](ingressDir)))

  // valid signals per output direction
  val shouldRoute = dontTouch(VecInit(routes.values.toSeq))

  // ready signals gated with routing select to make it one-hot
  val readies = routes.map { case (dir, shouldRoute) =>
    out(dir).valid := shouldRoute && in.valid
    out(dir).bits := in.bits
    shouldRoute && out(dir).ready
  }
  // combine one-hot ready signals to drive input ready
  in.ready := readies.reduce(_ || _)
  // data is forked
  out.values.foreach(_.bits := in.bits)
}
