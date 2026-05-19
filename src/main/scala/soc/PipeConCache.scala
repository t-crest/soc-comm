package soc

import chisel3._
import chisel3.util._

/**
  * A small direct-mapped, write-back data cache with PipeCon on both sides.
  *
  * The CPU side is a normal PipeCon slave port. The memory side is the same
  * protocol flipped around: this cache emits one-cycle rd/wr command pulses and
  * waits for memPort.ack.
  *
  * This simple version caches one 32-bit word per line. The lower two address
  * bits select byte lanes through wrMask, while tag/index are word based.
  */
class PipeConCache(addrWidth: Int, numLines: Int) extends PipeConDevice(addrWidth) {
  require(addrWidth >= 4, "PipeConCache expects byte addresses with room for tag/index")
  require(numLines >= 2 && isPow2(numLines), "numLines must be a power of two and at least 2")

  val indexBits = log2Ceil(numLines)
  val tagBits = addrWidth - indexBits - 2
  require(tagBits > 0, "addrWidth is too small for the requested number of cache lines")

  val memPort = IO(Flipped(new PipeCon(addrWidth)))

  val dataArray = Reg(Vec(numLines, UInt(32.W)))
  val tagArray = Reg(Vec(numLines, UInt(tagBits.W)))
  val validArray = RegInit(VecInit(Seq.fill(numLines)(false.B)))
  val dirtyArray = RegInit(VecInit(Seq.fill(numLines)(false.B)))

  val reqAddr = Reg(UInt(addrWidth.W))
  val reqWrite = Reg(Bool())
  val reqWdata = Reg(UInt(32.W))
  val reqMask = Reg(UInt(4.W))
  val evictTag = Reg(UInt(tagBits.W))
  val evictData = Reg(UInt(32.W))
  val ackData = RegInit(0.U(32.W))

  val sIdle :: sCpuAck :: sWriteBackCmd :: sWriteBackWait :: sRefillCmd :: sRefillWait :: sInstallWrite :: Nil = Enum(7)
  val state = RegInit(sIdle)

  def indexOf(addr: UInt): UInt = addr(indexBits + 1, 2)
  def tagOf(addr: UInt): UInt = addr(addrWidth - 1, indexBits + 2)
  def wordAddress(addr: UInt): UInt = Cat(addr(addrWidth - 1, 2), 0.U(2.W))
  def lineAddress(tag: UInt, index: UInt): UInt = Cat(tag, index, 0.U(2.W))

  def mergeBytes(oldData: UInt, newData: UInt, mask: UInt): UInt = {
    VecInit(Seq.tabulate(4) { i =>
      Mux(mask(i), newData(8 * i + 7, 8 * i), oldData(8 * i + 7, 8 * i))
    }).asUInt
  }

  cpuPort.ack := state === sCpuAck
  cpuPort.rdData := ackData

  memPort.address := 0.U
  memPort.rd := false.B
  memPort.wr := false.B
  memPort.wrData := 0.U
  memPort.wrMask := 0.U

  val fullWordWrite = reqMask === "b1111".U

  def installWrite(): Unit = {
    val idx = indexOf(reqAddr)
    val tag = tagOf(reqAddr)
    val merged = mergeBytes(0.U, reqWdata, reqMask)

    dataArray(idx) := merged
    tagArray(idx) := tag
    validArray(idx) := true.B
    dirtyArray(idx) := true.B
    ackData := merged
    state := sCpuAck
  }

  def startCpuCommand(): Unit = {
    val cpuCmd = cpuPort.rd || cpuPort.wr
    val idx = indexOf(cpuPort.address)
    val tag = tagOf(cpuPort.address)
    val hit = validArray(idx) && tagArray(idx) === tag
    val oldDirty = validArray(idx) && dirtyArray(idx)
    val writeData = mergeBytes(dataArray(idx), cpuPort.wrData, cpuPort.wrMask)

    when(cpuCmd) {
      reqAddr := cpuPort.address
      reqWrite := cpuPort.wr
      reqWdata := cpuPort.wrData
      reqMask := cpuPort.wrMask

      when(hit) {
        when(cpuPort.wr) {
          dataArray(idx) := writeData
          dirtyArray(idx) := true.B
          ackData := writeData
        }.otherwise {
          ackData := dataArray(idx)
        }
        state := sCpuAck
      }.otherwise {
        evictTag := tagArray(idx)
        evictData := dataArray(idx)
        when(oldDirty) {
          state := sWriteBackCmd
        }.elsewhen(cpuPort.wr && cpuPort.wrMask === "b1111".U) {
          dataArray(idx) := cpuPort.wrData
          tagArray(idx) := tag
          validArray(idx) := true.B
          dirtyArray(idx) := true.B
          ackData := cpuPort.wrData
          state := sCpuAck
        }.otherwise {
          state := sRefillCmd
        }
      }
    }
  }

  switch(state) {
    is(sIdle) {
      startCpuCommand()
    }

    is(sCpuAck) {
      state := sIdle
      startCpuCommand()
    }

    is(sWriteBackCmd) {
      memPort.address := lineAddress(evictTag, indexOf(reqAddr))
      memPort.wr := true.B
      memPort.wrData := evictData
      memPort.wrMask := "b1111".U
      state := sWriteBackWait
    }

    is(sWriteBackWait) {
      when(memPort.ack) {
        when(reqWrite && fullWordWrite) {
          state := sInstallWrite
        }.otherwise {
          state := sRefillCmd
        }
      }
    }

    is(sRefillCmd) {
      memPort.address := wordAddress(reqAddr)
      memPort.rd := true.B
      state := sRefillWait
    }

    is(sRefillWait) {
      when(memPort.ack) {
        val idx = indexOf(reqAddr)
        val refillData = Mux(reqWrite, mergeBytes(memPort.rdData, reqWdata, reqMask), memPort.rdData)

        dataArray(idx) := refillData
        tagArray(idx) := tagOf(reqAddr)
        validArray(idx) := true.B
        dirtyArray(idx) := reqWrite
        ackData := refillData
        state := sCpuAck
      }
    }

    is(sInstallWrite) {
      installWrite()
    }
  }
}
