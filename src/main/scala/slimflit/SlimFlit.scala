package slimflit

import chisel3._
import chisel3.util._
import slimflit._

class SlimFlit[T <: Data](p: SlimFlitParams[T]) extends Module {
  implicit val params: SlimFlitParams[T] = p
  def nocName: String = s"SlimFlit_${p.bufferFactory.name}_${p.arbiterFactory.name}"
  override val desiredName: String = nocName

  val ports = IO(MapBundle(
    p.coords, 
    new SlimFlitPort[T]
  ))
  val debugPorts = 
    if (p.debug) 
      Some(IO(MapBundle(
        p.coords, 
        new RouterDebugPort(new SlimPack[T])
      )))
    else None

  val routers = p.coords.map { coord =>
    val router = Module(new SlimRouter[T](coord))
    router.suggestName(s"router_${coord}")
    router.ports(Local).incoming <> ports(coord).ingress
    router.ports(Local).outgoing <> ports(coord).egress
    if (p.debug) {
      debugPorts.get(coord) <> router.debugPort.get
    }
    coord -> router
  }.toMap

  // connect routers according to the topology
  p.topology.connect(routers)
  
}
