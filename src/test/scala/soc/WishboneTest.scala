
package soc

import chisel.lib.fifo._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import s4noc.Entry

class WishboneTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The Wishbone example device"

  it should "do something" in {
    test(new HelloWishbone()) {
      d => {

        //        def step() = d.clock.step()

      }
    }
  }
}