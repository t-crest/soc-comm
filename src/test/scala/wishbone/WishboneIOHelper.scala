package wishbone

import chisel3._
import chiseltest._

class WishboneIOHelper(wb: WishboneIO, clock: Clock) {

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
      assert(time != timeOut, s"WB: time out on read after $time cycles")
    }
  }

  def read(addr: Int): BigInt = {
    // println(s"TB read $addr")
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
    // println(s"TB write, addr=$addr data=$data")
    wb.addr.poke(addr.U)
    wb.we.poke(true.B)
    wb.stb.poke(true.B)
    wb.cyc.poke(true.B)
    wb.wrData.poke(data.U)
    clockCnt += 1
    clock.step()
    wb.we.poke(false.B)
    waitForAck()
    wb.cyc.poke(false.B)
  }

  def setDest(n: Int) = write(2<<2, n)

  def getSender() = read(2<<2)

  // send and receive without status check, may block
  def send(data: BigInt) = write(1<<2, data)

  def receive = read(1<<2)

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
