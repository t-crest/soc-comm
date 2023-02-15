
package wishbone

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import s4noc._

class WishboneNoCTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "NoC Tester"

  "S4NoC in Wishbone" should "have a simple test" in {
    test(new S4NoCTopWB(Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32))) { d =>
      val helpSnd = new WishboneIOHelper(d.io.wbPorts(0), d.clock)
      val helpRcv = new WishboneIOHelper(d.io.wbPorts(3), d.clock)
      helpSnd.setDest(3)
      helpSnd.send(BigInt("cafebabe", 16))
      assert(helpRcv.receive == BigInt("cafebabe", 16))
    }
  }
}