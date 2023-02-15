package s4noc

import chiseltest._
import chiseltest.internal.TesterThreadList
import org.scalatest.flatspec.AnyFlatSpec
import soc.MemoryMappedIOHelper

class NocTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "NoC Tester"

  "S4NoC" should "have a simple test" in {
    test(new S4NoCTop(Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32))) { d =>
      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)
      helpSnd.send(BigInt("cafebabe", 16))
      assert(helpRcv.receive == BigInt("cafebabe", 16))
    }
  }

  it should "receive one packet, threaded" in {
    test(new S4NoCTop(Config(4, BubbleType(2), BubbleType(2), BubbleType(2), 32))) { d =>
      // Master thread (core 0)
      val helpSnd = new MemoryMappedIOHelper(d.io.cpuPorts(0), d.clock)
      val helpRcv = new MemoryMappedIOHelper(d.io.cpuPorts(3), d.clock)
      helpSnd.setDest(3)

      val th = fork {
        helpSnd.send(BigInt("cafebabe", 16))
        for (i <- 0 until 20) {
          helpSnd.step(1)
          // println("Master "+i + " " + helpSnd.getClockCnt)
        }
      }

      // this is the reader thread (core 3)
      var done = false
      for (i <- 0 until 14) {
        if (helpRcv.rxAvail) {
          assert(helpRcv.receive == BigInt("cafebabe", 16))
          assert(helpRcv.getSender() == 0)
          done = true
        }
      }
      assert(done)
      th.join()
    }
  }

  it should "have the correct sender ID in the IO register" in {
    val n = 4
    test(new S4NoCTop(Config(n, BubbleType(16), BubbleType(2), BubbleType(2), 32))) { d =>

      val help = new Array[MemoryMappedIOHelper](n)

      for (i <- 0 until n) help(i) = new MemoryMappedIOHelper(d.io.cpuPorts(i), d.clock)

      for (from <- 0 until n) {
        for (to <- 0 until n) {
          if (to != from) {
            help(from).setDest(to)
            help(from).send(from)
            val data = help(to).receive
            val sender = help(to).getSender()
            assert(data == sender)
          }
        }
      }
    }
  }


  // TODO: this test fails with BubbleType FIFOs
  it should "have the correct sender ID in the IO register, multi threaded" in {
    val n = 4
    test(new S4NoCTop(Config(n, MemType(16), MemType(2), MemType(2), 32))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>

      val threads = new Array[TesterThreadList](n)
      val help = new Array[MemoryMappedIOHelper](n)

      for (i <- 0 until n) {
        help(i) = new MemoryMappedIOHelper(d.io.cpuPorts(i), d.clock)
        threads(i) = fork {
          for (j <- 0 until n) {
            if (j != i) {
              help(i).setDest(j)
              help(i).send(i)
              println(s"send from $i to $j")
            }
          }
          for (j <- 0 until n - 1) {
            val data = help(i).receive
            val from = help(i).getSender()
            println(s"receive at $i from $from with $data")
            assert(data == from)
          }
        }
      }
      for (i <- 0 until n) {
        threads(i).join()
      }
    }
  }
}
