package slimflit

import chisel3._
import chisel3.util._

trait Topology {
  def name: String
  def connect[T <: Data](routers: Map[Coord, SlimRouter[T]]): Unit
  def routingPolicy: RoutingPolicy
}


object Mesh extends Topology {
  def name: String = "Mesh"
  def connect[T <: Data](routers: Map[Coord, SlimRouter[T]]): Unit = {
    for ((coord, router) <- routers) {
      val x = coord.x
      val y = coord.y
      val northCoord = Coord(x, y + 1)
      val southCoord = Coord(x, y - 1)
      val eastCoord = Coord(x + 1, y)
      val westCoord = Coord(x - 1, y)

      if (routers.contains(northCoord)) {
        router.port(North).outgoing <> routers(northCoord).port(South).incoming
        router.port(North).incoming <> routers(northCoord).port(South).outgoing
      } else {
        router.port(North).incoming.valid := 0.B
        router.port(North).incoming.bits := DontCare
        router.port(North).outgoing.ready := 1.B
      }
      if (routers.contains(southCoord)) {
        router.port(South).outgoing <> routers(southCoord).port(North).incoming
        router.port(South).incoming <> routers(southCoord).port(North).outgoing
      } else {
        router.port(South).incoming.valid := 0.B
        router.port(South).incoming.bits := DontCare
        router.port(South).outgoing.ready := 1.B
      }
      if (routers.contains(eastCoord)) {
        router.port(East).outgoing <> routers(eastCoord).port(West).incoming
        router.port(East).incoming <> routers(eastCoord).port(West).outgoing
      } else {
        router.port(East).incoming.valid := 0.B
        router.port(East).incoming.bits := DontCare
        router.port(East).outgoing.ready := 1.B
      }
      if (routers.contains(westCoord)) {
        router.port(West).outgoing <> routers(westCoord).port(East).incoming
        router.port(West).incoming <> routers(westCoord).port(East).outgoing
      } else {
        router.port(West).incoming.valid := 0.B
        router.port(West).incoming.bits := DontCare
        router.port(West).outgoing.ready := 1.B
      }
    }
  }

  def routingPolicy: RoutingPolicy = XYMeshRouting
}

object Torus extends Topology {
  def name: String = "Torus"
  def connect[T <: Data](routers: Map[Coord, SlimRouter[T]]): Unit = {
    for ((coord, router) <- routers) {
      val x = coord.x
      val y = coord.y
      val northCoord = Coord(x, (y + 1) % routers.size)
      val southCoord = Coord(x, (y - 1 + routers.size) % routers.size)
      val eastCoord = Coord((x + 1) % routers.size, y)
      val westCoord = Coord((x - 1 + routers.size) % routers.size, y)

      router.port(North).outgoing <> routers(northCoord).port(South).incoming
      router.port(North).incoming <> routers(northCoord).port(South).outgoing
      router.port(South).outgoing <> routers(southCoord).port(North).incoming
      router.port(South).incoming <> routers(southCoord).port(North).outgoing
      router.port(East).outgoing <> routers(eastCoord).port(West).incoming
      router.port(East).incoming <> routers(eastCoord).port(West).outgoing
      router.port(West).outgoing <> routers(westCoord).port(East).incoming
      router.port(West).incoming <> routers(westCoord).port(East).outgoing
    }
  }

  def routingPolicy: RoutingPolicy = XYTorusRouting
}