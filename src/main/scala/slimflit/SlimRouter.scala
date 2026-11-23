package slimflit

import chisel3._


class SlimRouter[T <: Data](val coord: Coord)(implicit p: SlimFlitParams[T]) extends Module {

  val ports = IO(new SlimRouterPorts(coord))
  def port(dir: Direction): SlimRouterPort[T] = ports(dir)

  val debugPort = if (p.debug) Some(IO(new RouterDebugPort(new SlimPack))) else None

  // create buffers for all 5 incoming ports
  val buffers = ports.incoming.map(port =>
    Module(p.bufferFactory.create(port, depth = p.bufferDepth)).suggestName(s"buffer_${port.dir}")
  )

  // connect incoming ports to buffers
  val bufferPorts = ports.incoming.zip(buffers).map { case (port, buf) =>
    port <> buf.enq
    buf.deq
  }

  // connect debug
  bufferPorts.foreach { bufOut =>
    if (p.debug) {
      debugPort.get.apply(bufOut.dir).valid := bufOut.valid
      debugPort.get.apply(bufOut.dir).packet := bufOut.bits
    }
  }

  // create routing logic for each input port
  val routingBlocks = ports.incoming.map { port =>
    Module(new RoutingBlock[T](port.dir, coord)).suggestName(s"routing_${port.dir}")
  }

  // connect debug
  routingBlocks.foreach { block => 
    if (p.debug) {
      debugPort.get.apply(block.ingressDir).reqs.values.foreach(_ := 0.B)
      block.out.values.foreach { case port =>
        debugPort.get.apply(block.ingressDir).reqs(port.dir) := port.valid
      }
    }
  }

  case class PacketInTransit[T <: Data](packetPort: SlimPackPort[T], from: Direction, to: Direction)

  // collect fanout of routing blocks
  val fanout = bufferPorts
    .zip(routingBlocks)
    .flatMap { case (bufOut, routingBlock) =>
      routingBlock.in <> bufOut
      routingBlock.out.map((to, port) => PacketInTransit(port, bufOut.dir, to))
    }

  // group requests by outgoing direction
  val egressRequests = fanout.groupBy(_.to)

  // create arbiters for each egress direction and connect them to the egress ports
  egressRequests.foreach { case (dir, reqPorts) =>
    val arbiter = Module(p // create arbiter
      .arbiterFactory.create(dir, reqPorts.map(_.from)))
      .suggestName(s"arbiter_${dir}")
    reqPorts // connect requests to arbiter inputs
      .foreach { case PacketInTransit(req, origin, _) =>
        req <> arbiter.in(origin)
      }
    // connect arbiter output to egress port
    ports(dir).outgoing <> arbiter.out
  }

}
