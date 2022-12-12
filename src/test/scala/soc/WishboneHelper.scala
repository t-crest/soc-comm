package soc

import chisel3._
import chiseltest._

class WishboneHelper(wb: WishboneIO, clock: Clock) {

  private var clockCnt = 0

  private var timeOut = 100
  def setTimeOut(t: Int) = timeOut = t

  def getClockCnt = clockCnt

  def step(n: Int): Unit = {
    clock.step(n)
    clockCnt += n
  }

  def waitForAck() = {
    if (!wb.ack.peekBoolean()) {
      var time = 0
      while (!wb.ack.peekBoolean() && (timeOut == -1 || time < timeOut)) {
        clock.step()
        clockCnt += 1
        time += 1
      }
      assert(time != timeOut, s"time out on read after $time cycles")
    }
  }

  def read(addr: Int): BigInt = {
    wb.addr.poke(addr.U)
    wb.we.poke(false.B)
    wb.stb.poke(true.B)
    wb.cyc.poke(true.B)
    clockCnt += 1
    clock.step()
    wb.stb.poke(false.B)
    waitForAck()
    wb.cyc.poke(false.B)
    wb.rdData.peekInt()
  }

  def write(addr: Int, data: BigInt) = {
    wb.addr.poke(addr.U)
    wb.we.poke(true.B)
    wb.stb.poke(true.B)
    wb.cyc.poke(true.B)
    clockCnt += 1
    clock.step()
    wb.we.poke(false.B)
    waitForAck()
    wb.cyc.poke(false.B)
  }
}
