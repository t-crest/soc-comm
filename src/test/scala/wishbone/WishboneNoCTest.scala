
package wishbone

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class WishboneNoCTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The Wishbone wrapped S4NOC"

  it should "work with the example device" in {
    test(new Hello()) {
      d => {
        def step() = d.clock.step()
      }
    }
  }
}