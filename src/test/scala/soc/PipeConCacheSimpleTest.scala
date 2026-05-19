package soc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class PipeConCacheSimpleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PipeConCache with a simple memory model"

  def initCpu(dut: PipeConCache): Unit = {
    dut.cpuPort.address.poke(0.U)
    dut.cpuPort.rd.poke(false.B)
    dut.cpuPort.wr.poke(false.B)
    dut.cpuPort.wrData.poke(0.U)
    dut.cpuPort.wrMask.poke(0.U)
  }

  class MemoryModel(dut: PipeConCache, initial: Map[Int, BigInt]) {
    val mem = mutable.Map(initial.toSeq: _*)
    var pending = false
    var pendingRead = false
    var pendingWrite = false
    var pendingAddress = 0
    var pendingWriteData = BigInt(0)

    dut.memPort.ack.poke(false.B)
    dut.memPort.rdData.poke(0.U)

    def step(): Unit = {
      if (pending) {
        dut.memPort.ack.poke(true.B)
        if (pendingWrite) {
          mem(pendingAddress) = pendingWriteData
        }
        dut.memPort.rdData.poke(mem.getOrElse(pendingAddress, BigInt(0)).U)
      } else {
        dut.memPort.ack.poke(false.B)
        dut.memPort.rdData.poke(0.U)
      }

      val nextRead = dut.memPort.rd.peekBoolean()
      val nextWrite = dut.memPort.wr.peekBoolean()
      val nextAddress = dut.memPort.address.peekInt().toInt
      val nextWriteData = dut.memPort.wrData.peekInt()

      dut.clock.step()

      pending = nextRead || nextWrite
      pendingRead = nextRead
      pendingWrite = nextWrite
      pendingAddress = nextAddress
      pendingWriteData = nextWriteData
    }
  }

  def read(dut: PipeConCache, mem: MemoryModel, address: Int): BigInt = {
    dut.cpuPort.address.poke(address.U)
    dut.cpuPort.rd.poke(true.B)
    dut.cpuPort.wr.poke(false.B)
    mem.step()
    dut.cpuPort.rd.poke(false.B)

    var cycles = 0
    while (!dut.cpuPort.ack.peekBoolean() && cycles < 20) {
      mem.step()
      cycles += 1
    }
    assert(cycles < 20, s"read from 0x${address.toHexString} timed out")

    val result = dut.cpuPort.rdData.peekInt()
    mem.step()
    result
  }

  it should "refill on a cache miss and hit on the second read" in {
    test(new PipeConCache(addrWidth = 8, numLines = 4)) { dut =>
      initCpu(dut)
      val mem = new MemoryModel(dut, Map(0x10 -> BigInt("12345678", 16)))

      assert(read(dut, mem, 0x10) == BigInt("12345678", 16))
      assert(read(dut, mem, 0x10) == BigInt("12345678", 16))
    }
  }
}
