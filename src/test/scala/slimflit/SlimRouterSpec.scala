package slimflit

import chisel3._
import chisel3.util._
import chiseltest._

import slimflit.buffers._
import slimflit.arbiter._

import org.scalatest.flatspec.AnyFlatSpec

class SlimRouterSpec extends AnyFlatSpec with ChiselScalatestTester {

  val annotations: firrtl.AnnotationSeq = Seq()

  implicit val config: SlimFlitParams[UInt] = SlimFlitParams[UInt](
    dim = (4,4),
    payloadGen = UInt(32.W),
    bufferFactory = GenericQueue,
    bufferDepth = 2,
    arbiterFactory = ChiselRRArbiter,
    topology = Mesh,
    debug = true
  )

  val home = Coord(2,2)

  def routerTest(
      sendPackets: => Map[Direction, Seq[SlimPack[UInt]]],
      expectedPackets: => Map[Direction, Seq[SlimPack[UInt]]]
  ) = {
    test(new SlimRouter(home)).withAnnotations(annotations) { dut =>
      
      val producers = for ((dir, packets) <- sendPackets) yield fork {
        val port = dut.ports(dir).incoming.initSource().setSourceClock(dut.clock)
        port.enqueueSeq(packets)
      }
      val consumers = for ((dir, packets) <- expectedPackets) yield fork {
        val port = dut.ports(dir).outgoing.initSink().setSinkClock(dut.clock)
        port.expectDequeueSeq(packets)
      }
      producers.foreach(_.join())
      consumers.foreach(_.join())
    }
  }

  "SlimFlit Router" should "route packets from north to south or local" in {
    lazy val southBound = config.createPacket(
      home -> Coord(2,3),
      payload = 0x1.U
    )
    lazy val localBound = config.createPacket(
      home -> Coord(2,2),
      payload = 0x2.U
    )

    routerTest(
      Map(
        North -> Seq(southBound, localBound)
      ),
      Map(
        South -> Seq(southBound),
        Local -> Seq(localBound)
      )
    )
  }

  it should "route packets from south to north or local" in {
    lazy val northBound = config.createPacket(
      home -> Coord(2,1),
      payload = 0x1.U
    )
    lazy val localBound = config.createPacket(
      home -> Coord(2,2),
      payload = 0x2.U
    )

    routerTest(
      Map(
        South -> Seq(northBound, localBound)
      ),
      Map(
        North -> Seq(northBound),
        Local -> Seq(localBound)
      )
    )
  }


  it should "route packets from east to north, south, west or local" in {
    lazy val westBound = config.createPacket(
      home -> Coord(1,2),
      payload = 0x5.U
    )
    lazy val southBound = config.createPacket(
      home -> Coord(2,3),
      payload = 0x7.U
    )
    lazy val northBound = config.createPacket(
      home -> Coord(2,1),
      payload = 0x8.U
    )
    lazy val localBound = config.createPacket(
      home -> Coord(2,2),
      payload = 0x6.U
    )
    routerTest(
      Map(
        East -> Seq(westBound, southBound, northBound, localBound)
      ),
      Map(
        West -> Seq(westBound),
        South -> Seq(southBound),
        North -> Seq(northBound),
        Local -> Seq(localBound)
      )
    )
  }

  it should "route packets from west to north, south, east or local" in {
    lazy val eastBound = config.createPacket(
      home -> Coord(3,2),
      payload = 0x9.U
    )
    lazy val southBound = config.createPacket(
      home -> Coord(2,3),
      payload = 0xB.U
    )
    lazy val northBound = config.createPacket(
      home -> Coord(2,1),
      payload = 0x8.U
    )
    lazy val localBound = config.createPacket(
      home -> Coord(2,2),
      payload = 0xA.U
    )
    routerTest(
      Map(
        West -> Seq(eastBound, southBound, northBound, localBound)
      ),
      Map(
        East -> Seq(eastBound),
        South -> Seq(southBound),
        North -> Seq(northBound),
        Local -> Seq(localBound)
      )
    )
  }

  it should "route packets from local to north, south, east or west" in {
    lazy val eastBound = config.createPacket(
      home -> Coord(3,2),
      payload = 0x1.U
    )
    lazy val southBound = config.createPacket(
      home -> Coord(2,3),
      payload = 0x2.U
    )
    lazy val northBound = config.createPacket(
      home -> Coord(2,1),
      payload = 0x3.U
    )
    lazy val westBound = config.createPacket(
      home -> Coord(1,2),
      payload = 0x4.U
    )
    routerTest(
      Map(
        Local -> Seq(eastBound, southBound, northBound, westBound)
      ),
      Map(
        East -> Seq(eastBound),
        South -> Seq(southBound),
        North -> Seq(northBound),
        West -> Seq(westBound)
      )
    )
  }
  
}
