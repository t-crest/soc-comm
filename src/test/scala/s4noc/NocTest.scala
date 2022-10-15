package s4noc

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import soc.MemoryMappedIOHelper

class NocTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Simple NoC Tester (just one packet)"

  "S4NoC" should "have a simple test" in {
    test(new S4NoCTop(Config(4, 2, 2, 2, 32))) { d =>
      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)
      helpSnd.send(BigInt("cafebabe", 16))
      assert(helpRcv.receive == BigInt("cafebabe", 16))
    }
  }

  it should "receive one packet, threaded" in {
    test(new S4NoCTop(Config(4, 2, 2, 2, 32))) { d =>
      // Master thread (core 0)
      // Write into time slot 0 to reach core 3
      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)

      val th = fork {
        helpSnd.send(BigInt("cafebabe", 16))
        for (i <- 0 until 20) {
          helpSnd.step(1)
          println("Master "+i + " " + helpSnd.getClockCnt)
        }
      }

      // this is the reader thread (core 3)
      var done = false
      for (i <- 0 until 14) {
        if (helpRcv.rxAvail) {
          assert(helpRcv.receive == BigInt("cafebabe", 16))
          assert(helpRcv.getSender() == 0)
          println("Got a packet")
          done = true
        }
      }
      assert(done)
      th.join()
    }
  }
}
