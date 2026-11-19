package slimflit

import chisel3._

/**
  * A routing policy defines rules depending on the ingress direction of a packet.
  * 
  * The rule generates routing logic 
  */
trait RoutingPolicy {
  /** Returns the routing rule for the given ingress direction. */
  def getRule(ingressDir: Direction): RoutingRule

  /**
    * Generates routing logic for a packet based on its input port, destination and the current location.
    *
    * @param here The coordinates of the router.
    * @param ingressDir The direction from which the packet is incoming.
    * @param dest The destination of the packet.
    * @return A one-hot map of output directions to Bool wires.
    */
  def generateLogic(here: Coord, ingressDir: Direction, dest: Destination): Map[Direction, Bool] = {
    getRule(ingressDir).generateLogic(dest, here)
  }


  /**
    * Creates a new packet for the given source and destination coordinates and payload.
    *
    * @param fromto A tuple containing the source and destination coordinates.
    * @param payload The payload of the packet.
    * @return The new SlimPack literal.
    */
  def createPacket[T <: Data](fromto: (Coord, Coord), payload: T)(implicit p: SlimFlitParams[T]): SlimPack[T]
}


/**
  * A routing rule generates routing logic for an
  * incoming packet based on the current location.
  * 
  */
trait RoutingRule {
  def generateLogic(dest: Destination, here: Coord): Map[Direction, Bool]
}


object XYMeshRouting extends XYRouting(torus = false)
object XYTorusRouting extends XYRouting(torus = true)

class XYRouting(torus: Boolean) extends RoutingPolicy {

  def createPacket[T <: Data](fromto: (Coord, Coord), payload: T)(implicit p: SlimFlitParams[T]): SlimPack[T] = {
    val (from, to) = fromto
    if (torus) {
      val dx = (to.x - from.x + p.nx) % p.nx
      val dy = (to.y - from.y + p.ny) % p.ny
      val eastbound = true // dx <= p.nx / 2
      val southbound = true // dy <= p.ny / 2
      SlimPack.Lit(to, eastbound, southbound, payload)
    } else {
      val southbound = to.y > from.y
      val eastbound = to.x > from.x
      SlimPack.Lit(to, eastbound, southbound, payload)
    }
  }

  def getRule(ingressDir: Direction): RoutingRule = ingressDir match {
    case East | West   => HorizontalRule(ingressDir)
    case North | South => VerticalRule(ingressDir)
    case Local         => LocalRule
  }

  /** Direction-order routing: first along x then y
    *
    *   1. full match -> local
    *   2. no x match -> continue in ingress direction
    *   3. no y match -> turn left or right based on southbound
    *
    * @param ingressDir
    */
  case class HorizontalRule(ingressDir: Direction) extends RoutingRule {
    def generateLogic(dest: Destination, here: Coord): Map[Direction, Bool] = {
      val xMatch = dest.x === here.x.U
      val yMatch = dest.y === here.y.U
      val turnLeft = ingressDir match {
        case East => !dest.southbound
        case West => dest.southbound
        case _    =>
          throw new Exception(
            "Invalid ingress direction for HorizontalIngressRule"
          )
      }
      Map(
        Local -> (xMatch && yMatch),
        ingressDir.opposite -> !xMatch,
        ingressDir.rotateNegative -> (xMatch && !yMatch && !turnLeft),
        ingressDir.rotatePositive -> (xMatch && !yMatch && turnLeft)
      )
    }
  }

  /** Direction-order routing: first along x then y
    *
    *   1. y match -> local
    *   2. no y match -> continue in direction opposite to ingress
    *
    * @param ingressDir
    */
  case class VerticalRule(ingressDir: Direction) extends RoutingRule {
    def generateLogic(dest: Destination, here: Coord): Map[Direction, Bool] = {
      val yMatch = dest.y === here.y.U
      Map(
        Local -> yMatch,
        ingressDir.opposite -> !yMatch
      )
    }
  }

  /** Direction-order routing: first along x then y
    *
    *   1. no x match -> go west or east based on eastbound
    *   2. x match -> go north or south based on southbound
    */
  case object LocalRule extends RoutingRule {
    def generateLogic(dest: Destination, here: Coord): Map[Direction, Bool] = {
      val xMatch = dest.x === here.x.U
      Map(
        East -> (!xMatch && dest.eastbound),
        West -> (!xMatch && !dest.eastbound),
        South -> (xMatch && dest.southbound),
        North -> (xMatch && !dest.southbound)
      )
    }
  }
}
