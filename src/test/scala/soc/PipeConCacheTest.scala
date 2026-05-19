package soc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class PipeConCacheTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The PipeConCache"

  def mergeBytes(oldData: BigInt, newData: BigInt, mask: BigInt): BigInt = {
    (0 until 4).foldLeft(oldData) { (acc, i) =>
      if (((mask >> i) & 1) == 1) {
        val byte = (newData >> (8 * i)) & 0xff
        (acc & ~(BigInt(0xff) << (8 * i))) | (byte << (8 * i))
      } else {
        acc
      }
    } & 0xffffffffL
  }

  class PipeConMemoryModel(dut: PipeConCache, initial: Map[Int, BigInt]) {
    val memory: mutable.Map[Int, BigInt] = mutable.Map(initial.toSeq: _*)
    var reads = 0
    var writes = 0
    var pending = false
    var pendingWrite = false
    var pendingAddress = 0
    var pendingWriteData = BigInt(0)
    var pendingMask = BigInt(0)

    dut.memPort.ack.poke(false.B)
    dut.memPort.rdData.poke(0.U)

    def cycle(): Unit = {
      if (pending) {
        dut.memPort.ack.poke(true.B)
        if (pendingWrite) {
          val oldData = memory.getOrElse(pendingAddress, BigInt(0))
          memory(pendingAddress) = mergeBytes(oldData, pendingWriteData, pendingMask)
          writes += 1
        }
        dut.memPort.rdData.poke(memory.getOrElse(pendingAddress, BigInt(0)).U)
      } else {
        dut.memPort.ack.poke(false.B)
        dut.memPort.rdData.poke(0.U)
      }

      val nextRead = dut.memPort.rd.peekBoolean()
      val nextWrite = dut.memPort.wr.peekBoolean()
      val nextAddress = dut.memPort.address.peekInt().toInt
      val nextWriteData = dut.memPort.wrData.peekInt()
      val nextMask = dut.memPort.wrMask.peekInt()

      if (nextRead) reads += 1
      dut.clock.step()

      pending = nextRead || nextWrite
      pendingWrite = nextWrite
      pendingAddress = nextAddress
      pendingWriteData = nextWriteData
      pendingMask = nextMask
    }
  }

  object PipeConMemoryModel {
    def fromArray(dut: PipeConCache, words: Seq[BigInt], baseAddress: Int = 0): PipeConMemoryModel = {
      val initial = words.zipWithIndex.map { case (word, i) =>
        (baseAddress + 4 * i) -> (word & 0xffffffffL)
      }.toMap
      new PipeConMemoryModel(dut, initial)
    }
  }

  def initCpu(dut: PipeConCache): Unit = {
    dut.cpuPort.address.poke(0.U)
    dut.cpuPort.rd.poke(false.B)
    dut.cpuPort.wr.poke(false.B)
    dut.cpuPort.wrData.poke(0.U)
    dut.cpuPort.wrMask.poke(0.U)
  }

  def read(dut: PipeConCache, mem: PipeConMemoryModel, address: Int): BigInt = {
    dut.cpuPort.address.poke(address.U)
    dut.cpuPort.rd.poke(true.B)
    dut.cpuPort.wr.poke(false.B)
    mem.cycle()
    dut.cpuPort.rd.poke(false.B)

    var waitCycles = 0
    while (!dut.cpuPort.ack.peekBoolean() && waitCycles < 20) {
      mem.cycle()
      waitCycles += 1
    }
    assert(waitCycles < 20, s"read from 0x${address.toHexString} timed out")
    val result = dut.cpuPort.rdData.peekInt()
    mem.cycle()
    result
  }

  def write(dut: PipeConCache, mem: PipeConMemoryModel, address: Int, data: BigInt, mask: Int = 0xf): Unit = {
    dut.cpuPort.address.poke(address.U)
    dut.cpuPort.wrData.poke(data.U)
    dut.cpuPort.wrMask.poke(mask.U)
    dut.cpuPort.wr.poke(true.B)
    dut.cpuPort.rd.poke(false.B)
    mem.cycle()
    dut.cpuPort.wr.poke(false.B)

    var waitCycles = 0
    while (!dut.cpuPort.ack.peekBoolean() && waitCycles < 20) {
      mem.cycle()
      waitCycles += 1
    }
    assert(waitCycles < 20, s"write to 0x${address.toHexString} timed out")
    mem.cycle()
  }

  it should "refill on a read miss and hit on a repeated read" in {
    test(new PipeConCache(addrWidth = 8, numLines = 4)) { dut =>
      initCpu(dut)
      val mem = new PipeConMemoryModel(dut, Map(0x10 -> BigInt("12345678", 16)))
      mem.cycle()

      assert(read(dut, mem, 0x10) == BigInt("12345678", 16))
      assert(mem.reads == 1)

      assert(read(dut, mem, 0x10) == BigInt("12345678", 16))
      assert(mem.reads == 1, "second read should hit without another memory read")
    }
  }

  it should "fill a missing line from an array-backed simulated memory" in {
    test(new PipeConCache(addrWidth = 8, numLines = 4)) { dut =>
      initCpu(dut)
      val mem = PipeConMemoryModel.fromArray(
        dut,
        Seq(
          BigInt("00112233", 16),
          BigInt("44556677", 16),
          BigInt("8899aabb", 16),
          BigInt("ccddeeff", 16)
        )
      )
      mem.cycle()

      assert(read(dut, mem, 0x08) == BigInt("8899aabb", 16))
      assert(mem.reads == 1)

      assert(read(dut, mem, 0x08) == BigInt("8899aabb", 16))
      assert(mem.reads == 1, "array-backed fill should be cached after the first miss")
    }
  }

  it should "merge byte writes on a hit" in {
    test(new PipeConCache(addrWidth = 8, numLines = 4)) { dut =>
      initCpu(dut)
      val mem = new PipeConMemoryModel(dut, Map(0x04 -> BigInt("11223344", 16)))
      mem.cycle()

      assert(read(dut, mem, 0x04) == BigInt("11223344", 16))
      write(dut, mem, 0x04, BigInt("0000aabb", 16), mask = 0x3)

      assert(read(dut, mem, 0x04) == BigInt("1122aabb", 16))
      assert(mem.memory(0x04) == BigInt("11223344", 16), "write-back cache should not update memory until eviction")
    }
  }

  it should "write back a dirty line before replacing it" in {
    test(new PipeConCache(addrWidth = 8, numLines = 4)) { dut =>
      initCpu(dut)
      val mem = new PipeConMemoryModel(
        dut,
        Map(
          0x00 -> BigInt("11111111", 16),
          0x10 -> BigInt("22222222", 16)
        )
      )
      mem.cycle()

      write(dut, mem, 0x00, BigInt("aabbccdd", 16))
      assert(mem.writes == 0, "full-word write miss should allocate dirty data without immediate write-through")
      assert(read(dut, mem, 0x00) == BigInt("aabbccdd", 16))

      assert(read(dut, mem, 0x10) == BigInt("22222222", 16))
      assert(mem.writes == 1)
      assert(mem.memory(0x00) == BigInt("aabbccdd", 16))
    }
  }
}
