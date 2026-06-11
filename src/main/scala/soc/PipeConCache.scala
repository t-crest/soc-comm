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

  val memPort = IO(Flipped(new PipeConIO(addrWidth)))

  val dataMem = SyncReadMem(numLines, UInt(32.W))
  val tagMem = SyncReadMem(numLines, UInt(tagBits.W))
  val validArray = RegInit(VecInit(Seq.fill(numLines)(false.B)))
  val dirtyArray = RegInit(VecInit(Seq.fill(numLines)(false.B)))

  val reqAddr = Reg(UInt(addrWidth.W))
  val reqWrite = Reg(Bool())
  val reqWdata = Reg(UInt(32.W))
  val reqMask = Reg(UInt(4.W))
  val evictTag = Reg(UInt(tagBits.W))
  val evictData = Reg(UInt(32.W))
  val ackData = RegInit(0.U(32.W))

  val sIdle :: sLookup :: sCpuAck :: sWriteBackCmd :: sWriteBackWait :: sRefillCmd :: sRefillWait :: sInstallWrite :: Nil = Enum(8)
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

  val cpuCmd = cpuPort.rd || cpuPort.wr
  val lookupRead = (state === sIdle || state === sLookup || state === sCpuAck) && cpuCmd
  val lookupIndex = indexOf(cpuPort.address)
  val lookupData = dataMem.read(lookupIndex, lookupRead)
  val lookupTag = tagMem.read(lookupIndex, lookupRead)
  val lookupReqIndex = indexOf(reqAddr)
  val lookupReqTag = tagOf(reqAddr)
  val lookupValid = validArray(lookupReqIndex)
  val lookupHit = state === sLookup && lookupValid && lookupTag === lookupReqTag
  val lookupOldDirty = lookupValid && dirtyArray(lookupReqIndex)
  val lookupFullWriteAllocate = state === sLookup && !lookupHit && !lookupOldDirty && reqWrite && reqMask === "b1111".U
  val lookupWriteData = mergeBytes(lookupData, reqWdata, reqMask)

  cpuPort.ack := state === sCpuAck || lookupHit || lookupFullWriteAllocate
  cpuPort.rdData := Mux(lookupHit, Mux(reqWrite, lookupWriteData, lookupData), Mux(lookupFullWriteAllocate, reqWdata, ackData))

  memPort.address := 0.U
  memPort.rd := false.B
  memPort.wr := false.B
  memPort.wrData := 0.U
  memPort.wrMask := 0.U

  val fullWordWrite = reqMask === "b1111".U

  def installWrite(): Unit = {
    val idx = indexOf(reqAddr)
    val tag = tagOf(reqAddr)

    dataMem.write(idx, reqWdata)
    tagMem.write(idx, tag)
    validArray(idx) := true.B
    dirtyArray(idx) := true.B
    ackData := reqWdata
    state := sCpuAck
  }

  def startCpuCommand(): Unit = {
    when(cpuCmd) {
      reqAddr := cpuPort.address
      reqWrite := cpuPort.wr
      reqWdata := cpuPort.wrData
      reqMask := cpuPort.wrMask
      state := sLookup
    }
  }

  def finishLookup(): Unit = {
    when(lookupHit) {
      when(reqWrite) {
        dataMem.write(lookupReqIndex, lookupWriteData)
        dirtyArray(lookupReqIndex) := true.B
      }
      when(cpuCmd) {
        reqAddr := cpuPort.address
        reqWrite := cpuPort.wr
        reqWdata := cpuPort.wrData
        reqMask := cpuPort.wrMask
        state := sLookup
      }.otherwise {
        state := sIdle
      }
    }.otherwise {
      evictTag := lookupTag
      evictData := lookupData
      when(lookupOldDirty) {
        state := sWriteBackCmd
      }.elsewhen(reqWrite && reqMask === "b1111".U) {
        dataMem.write(lookupReqIndex, reqWdata)
        tagMem.write(lookupReqIndex, lookupReqTag)
        validArray(lookupReqIndex) := true.B
        dirtyArray(lookupReqIndex) := true.B
        when(cpuCmd) {
          reqAddr := cpuPort.address
          reqWrite := cpuPort.wr
          reqWdata := cpuPort.wrData
          reqMask := cpuPort.wrMask
          state := sLookup
        }.otherwise {
          state := sIdle
        }
      }.otherwise {
        state := sRefillCmd
      }
    }
  }

  switch(state) {
    is(sIdle) {
      startCpuCommand()
    }

    is(sLookup) {
      finishLookup()
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

        dataMem.write(idx, refillData)
        tagMem.write(idx, tagOf(reqAddr))
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
