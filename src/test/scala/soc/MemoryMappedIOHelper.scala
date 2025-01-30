package soc

import chisel3._
import chiseltest._
/**
 * Helper class to read and write from a memory mapped IO device (PipeCon)
 * @param mmio
 * @param clock
 */
class MemoryMappedIOHelper(mmio: PipeCon, clock: Clock) {

  private var clockCnt = 0

  private var timeOut = 100
  def setTimeOut(t: Int) = timeOut = t

  def getClockCnt = clockCnt

  def step(n: Int): Unit = {
    clock.step(n)
    clockCnt += n
  }

  def waitForAck() = {
    if (!mmio.ack.peekBoolean()) {
      var time = 0
      while (!mmio.ack.peekBoolean() && (timeOut == -1 || time < timeOut)) {
        clock.step()
        clockCnt += 1
        time += 1
      }
      assert(time != timeOut, s"time out on read after $time cycles")
    }
  }

  def read(addr: Int): BigInt = {
    mmio.address.poke(addr.U)
    mmio.wr.poke(false.B)
    mmio.rd.poke(true.B)
    clockCnt += 1
    clock.step()
    mmio.rd.poke(false.B)
    waitForAck()
    mmio.rdData.peekInt()
  }

  def write(addr: Int, data: BigInt) = {
    mmio.address.poke(addr.U)
    mmio.wrData.poke(data.U)
    mmio.wr.poke(true.B)
    mmio.rd.poke(false.B)
    clockCnt += 1
    clock.step()
    mmio.wr.poke(false.B)
    waitForAck()
  }

  def setDest(n: Int) = write(8, n)
  def getSender() = read(8)

  // send and receive without status check, may block
  def send(data: BigInt) = write(4, data)
  def receive = read(4)

  // The following functions use the IO mapping with status bits add address 0 and data at address 1
  def txRdy = (read(0) & 0x01) != 0
  def rxAvail = (read(0) & 0x02) != 0

  def sndWithCheck(data: BigInt) = {
    while (!txRdy) {}
    send(data)
  }

  def rcvWithCheck() = {
    while (!rxAvail) {}
    receive
  }
}
