package soc

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class PipeConArbiterTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "The PipeConArbiter"

  class PipeConMemoryModel(dut: PipeConArbiter, initial: Map[Int, BigInt]) {
    val memory: mutable.Map[Int, BigInt] = mutable.Map(initial.toSeq: _*)
    val readAddresses = mutable.ArrayBuffer[Int]()
    val writeAddresses = mutable.ArrayBuffer[Int]()
    var pending = false
    var pendingWrite = false
    var pendingAddress = 0
    var pendingWriteData = BigInt(0)
    var pendingMask = BigInt(0)

    dut.memPort.ack.poke(false.B)
    dut.memPort.rdData.poke(0.U)

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

    def cycle(): Unit = {
      if (pending) {
        dut.memPort.ack.poke(true.B)
        if (pendingWrite) {
          val oldData = memory.getOrElse(pendingAddress, BigInt(0))
          memory(pendingAddress) = mergeBytes(oldData, pendingWriteData, pendingMask)
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

      if (nextRead) readAddresses += nextAddress
      if (nextWrite) writeAddresses += nextAddress

      dut.clock.step()

      pending = nextRead || nextWrite
      pendingWrite = nextWrite
      pendingAddress = nextAddress
      pendingWriteData = nextWriteData
      pendingMask = nextMask
    }
  }

  def initCpu(dut: PipeConArbiter): Unit = {
    for (port <- dut.cpuPorts) {
      port.address.poke(0.U)
      port.rd.poke(false.B)
      port.wr.poke(false.B)
      port.wrData.poke(0.U)
      port.wrMask.poke(0.U)
    }
  }

  def readPulse(dut: PipeConArbiter, port: Int, address: Int): Unit = {
    dut.cpuPorts(port).address.poke(address.U)
    dut.cpuPorts(port).rd.poke(true.B)
    dut.cpuPorts(port).wr.poke(false.B)
  }

  def writePulse(dut: PipeConArbiter, port: Int, address: Int, data: BigInt, mask: Int = 0xf): Unit = {
    dut.cpuPorts(port).address.poke(address.U)
    dut.cpuPorts(port).wrData.poke(data.U)
    dut.cpuPorts(port).wrMask.poke(mask.U)
    dut.cpuPorts(port).wr.poke(true.B)
    dut.cpuPorts(port).rd.poke(false.B)
  }

  def clearPulse(dut: PipeConArbiter, port: Int): Unit = {
    dut.cpuPorts(port).rd.poke(false.B)
    dut.cpuPorts(port).wr.poke(false.B)
  }

  def waitForAck(dut: PipeConArbiter, mem: PipeConMemoryModel, port: Int): BigInt = {
    var waitCycles = 0
    while (!dut.cpuPorts(port).ack.peekBoolean() && waitCycles < 20) {
      mem.cycle()
      waitCycles += 1
    }
    assert(waitCycles < 20, s"port $port timed out")
    val data = dut.cpuPorts(port).rdData.peekInt()
    mem.cycle()
    data
  }

  it should "capture simultaneous read pulses and return each response to the requester" in {
    test(new PipeConArbiter(addrWidth = 8, nrPorts = 2)) { dut =>
      initCpu(dut)
      val mem = new PipeConMemoryModel(
        dut,
        Map(
          0x04 -> BigInt("11112222", 16),
          0x08 -> BigInt("33334444", 16)
        )
      )
      mem.cycle()

      readPulse(dut, 0, 0x04)
      readPulse(dut, 1, 0x08)
      mem.cycle()
      clearPulse(dut, 0)
      clearPulse(dut, 1)

      assert(waitForAck(dut, mem, 0) == BigInt("11112222", 16))
      assert(waitForAck(dut, mem, 1) == BigInt("33334444", 16))
      assert(mem.readAddresses.toSeq == Seq(0x04, 0x08))
    }
  }

  it should "forward writes from competing ports one at a time" in {
    test(new PipeConArbiter(addrWidth = 8, nrPorts = 2)) { dut =>
      initCpu(dut)
      val mem = new PipeConMemoryModel(dut, Map.empty)
      mem.cycle()

      writePulse(dut, 0, 0x00, BigInt("aaaabbbb", 16))
      writePulse(dut, 1, 0x04, BigInt("ccccdddd", 16))
      mem.cycle()
      clearPulse(dut, 0)
      clearPulse(dut, 1)

      waitForAck(dut, mem, 0)
      waitForAck(dut, mem, 1)

      assert(mem.writeAddresses.toSeq == Seq(0x00, 0x04))
      assert(mem.memory(0x00) == BigInt("aaaabbbb", 16))
      assert(mem.memory(0x04) == BigInt("ccccdddd", 16))
    }
  }
}
