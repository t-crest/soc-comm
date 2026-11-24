
import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import slimflit.buffers._
import slimflit.arbiter._

import scala.collection.immutable.SeqMap

package slimflit {

 


  /**
    * Configuration parameters for a SlimFlit network.
    *
    * @param dim Dimensions of the network (x, y)
    * @param payloadGen A function that generates a new payload of type T
    * @param bufferFactory Configure the buffer type
    * @param bufferDepth Depth of the buffer
    * @param arbiterFactory Configure the arbiter type
    * @param routingPolicy Configure the routing policy
    */
  class SlimFlitParams[T <: Data](
    val dim: (Int, Int),
    val payloadGen: () => T,
    val bufferFactory: BufferFactory,
    val bufferDepth: Int = 1,
    val arbiterFactory: ArbiterFactory,
    val topology: Topology,
    val debug: Boolean = false
  ) {
    /** The x dimension of the network. */
    def nx = dim._1

    /** The y dimension of the network. */
    def ny = dim._2

    /** The UInt type for the x coordinate. */
    def xCoordType = UInt(log2Ceil(nx).W)

    /** The UInt type for the y coordinate. */
    def yCoordType = UInt(log2Ceil(ny).W)

    /** The type of the payload. */
    def payloadType = payloadGen()

    def coords: Seq[Coord] = for {
      x <- 0 until nx
      y <- 0 until ny
    } yield Coord(x, y)

    def routingPolicy: RoutingPolicy = topology.routingPolicy

    def createPacket(fromto: (Coord, Coord), payload: T): SlimPack[T] = {
      routingPolicy.createPacket(fromto, payload)(this)
    }

  }

  object SlimFlitParams {
    def apply[T <: Data](
      dim: (Int, Int),
      payloadGen: => T,
      bufferFactory: BufferFactory,
      bufferDepth: Int = 1,
      arbiterFactory: ArbiterFactory,
      topology: Topology,
      debug: Boolean = false
    ): SlimFlitParams[T] = new SlimFlitParams(dim, () => payloadGen, bufferFactory, bufferDepth, arbiterFactory, topology, debug)
  }

  /** Coordinates of a node in the network. */
  case class Coord(x: Int, y: Int)

  /**
    * Directions in the network, used for routing and packet movement.
    */    
  sealed trait Direction {
    def toInt: Int = this match {
      case North => 0
      case East  => 1
      case South => 2
      case West  => 3
      case Local => 4
    }
    override def toString: String = this match {
      case North => "N"
      case East  => "E"
      case South => "S"
      case West  => "W"
      case Local => "L"
    }
    def opposite: Direction = this match {
      case North => South
      case East  => West
      case South => North
      case West  => East
      case Local => Local
    }
    def rotateNegative: Direction = this match {
      case North => East
      case East  => South
      case South => West
      case West  => North
      case Local => Local
    }
    def rotatePositive: Direction = this match {
      case North => West
      case East  => North
      case South => East
      case West  => South
      case Local => Local
    }
  }

  // Directions
  case object North extends Direction
  case object East extends Direction
  case object South extends Direction
  case object West extends Direction
  case object Local extends Direction

  object Direction {
    val all: Seq[Direction] = Seq(North, East, South, West, Local)
    val count: Int = all.length
    def fromInt(i: Int): Direction = i match {
      case 0 => North
      case 1 => East
      case 2 => South
      case 3 => West
      case 4 => Local
      case _ => throw new Exception(s"Invalid integer for Direction: $i")
    }
  }

  /** 
    * Destination of a packet in the network. 
    *  
    * The destination is represented by its coordinates and the direction of the packet
    * encoded in the eastbound and southbound signals.
    *
    */
  class Destination(implicit p: SlimFlitParams[_]) extends Bundle {
    val x = p.xCoordType
    val y = p.yCoordType
    val eastbound = Bool()
    val southbound = Bool()
  }


  /**
    * A packet in the SlimFlit network.
    * 
    * Contains routing information and payload.
    */
  class SlimPack[T <: Data](implicit p: SlimFlitParams[T]) extends Bundle {
    val dest = new Destination
    val payload = p.payloadType
  }

  object SlimPack {
    /**
      * Creates a new SlimPack literal with the given destination, direction, and payload.
      *
      * @param dest The destination coordinates of the packet.
      * @param eastbound The direction of the packet in the x-axis.
      * @param southbound The direction of the packet in the y-axis.
      * @param payload The payload of the packet.
      * @return The new SlimPack literal.
      */
    def Lit[T <: Data](dest: Coord, eastbound: Boolean, southbound: Boolean, payload: => T)(implicit p: SlimFlitParams[T]): SlimPack[T] = {
      (new SlimPack()).Lit(
        _.dest.x -> dest.x.U,
        _.dest.y -> dest.y.U,
        _.dest.eastbound -> eastbound.B,
        _.dest.southbound -> southbound.B,
        _.payload -> payload
      )
    }
  }


  class SlimPackPort[T <: Data](val dir: Direction)(implicit p: SlimFlitParams[T]) extends DecoupledIO(new SlimPack[T])

  class SlimRouterPort[T <: Data](val coord: Coord, val dir: Direction)(implicit p: SlimFlitParams[T]) extends Bundle {
    val incoming = Flipped(new SlimPackPort[T](dir)).suggestName(s"in_${dir}")
    val outgoing = new SlimPackPort[T](dir).suggestName(s"out_${dir}")

    def <>(that: SlimRouterPort[T]): Unit = {
      this.incoming <> that.incoming
      this.outgoing <> that.outgoing
    }
  }

  class SlimFlitPort[T <: Data](implicit p: SlimFlitParams[T]) extends Bundle {
    val ingress = Flipped(new SlimPackPort[T](Local))
    val egress = new SlimPackPort[T](Local)
  }

  class SlimRouterPorts[T <: Data](val coord: Coord)(implicit p: SlimFlitParams[T]) extends Bundle {
    val north = new SlimRouterPort[T](coord, North)
    val east = new SlimRouterPort[T](coord, East)
    val south = new SlimRouterPort[T](coord, South)
    val west = new SlimRouterPort[T](coord, West)
    val local = new SlimRouterPort[T](coord, Local)

    def all: Seq[SlimRouterPort[T]] = Seq(north, east, south, west, local)
    def apply(dir: Direction): SlimRouterPort[T] = dir match {
      case North => north
      case East  => east
      case South => south
      case West  => west
      case Local => local
    }
    def incoming: Seq[SlimPackPort[T]] = all.map(_.incoming)
    def outgoing: Seq[SlimPackPort[T]] = all.map(_.outgoing)
  }


  class MapBundle[K, V <: Data](ks: Seq[K], gen: K => V) extends Record {
    val signalMap = ks.map(k => k -> gen(k)).toMap
    override def elements: SeqMap[String,Data] = SeqMap(signalMap.toSeq.map { case (k, signal) => 
      k.toString -> signal 
    }:_*)
    def apply(k: K): V = this.signalMap(k)
    def keys: Seq[K] = signalMap.keys.toSeq
    def values: Seq[V] = signalMap.values.toSeq
    def <>(that: MapBundle[K, V]): Unit = {
      require(this.keys.toSet == that.keys.toSet, "MapBundles must have the same keys to connect")
      for (k <- keys) {
        this(k) <> that(k)
      }
    }
    def map[U](f: (K, V) => U): Seq[U] = {
      signalMap.map { case (k, v) => f(k, v) }.toSeq
    }
  }

  object MapBundle {
    def apply[K, V <: Data](ks: Seq[K], gen: K => V): MapBundle[K, V] = new MapBundle(ks, gen)
    def apply[K, V <: Data](ks: Seq[K], gen: => V): MapBundle[K, V] = new MapBundle(ks, _ => gen)
  }

  class RouterDebugElement[P <: Data](packetGen: => P) extends Bundle {
    val reqs = MapBundle(Direction.all, Bool())
    val valid = Bool()
    val packet = packetGen
  }
  class RouterDebugPort[P <: Data](packetGen: => P) extends MapBundle[Direction, RouterDebugElement[P]](Direction.all, _ => new RouterDebugElement(packetGen)){

  }


}