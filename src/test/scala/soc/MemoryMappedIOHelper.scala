package soc

import chisel3._
import chiseltest._

class MemoryMappedIOHelper(mmio: MemoryMappedIO, clock: Clock) {

  private var clockCnt = 0

  private var timeOut = 10
  def setTimeOut(t: Int) = timeOut = t

  def getClockCnt = clockCnt

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
  // The folloiwng functions use the IO mapping with status bits add address 0 and data at address 1
  def txRdy = (read(0) & 0x01) != 0
  def rxAvail = (read(0) & 0x02) != 0
  def send(data: BigInt) = write(1, data)
  def receive = read(1)

  def sndWithCheck(data: BigInt) = {
    while (!txRdy) {}
    send(data)
  }

  def rcvWithCheck() = {
    while (!rxAvail) {}
    receive
  }
}
