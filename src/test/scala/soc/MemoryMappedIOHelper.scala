package soc

import chisel3._
import chiseltest._

class MemoryMappedIOHelper(cpif: CpuInterfaceOnly) {

  val cp = cpif.io.cpuPort

  var timeOut = -1
  def setTimeOut(t: Int) = timeOut = t

  def waitForAck() = {
    if (!cp.ack.peekBoolean()) {
      var time = 0
      while (!cp.ack.peekBoolean() && (timeOut == -1 || time < timeOut)) {
        cpif.clock.step()
        time += 1
      }
      assert(time != timeOut, s"time out on read after $time cycles")
    }
  }

  def read(addr: Int): BigInt = {
    cp.address.poke(addr.U)
    cp.wr.poke(false.B)
    cp.rd.poke(true.B)
    cpif.clock.step()
    cp.rd.poke(false.B)
    waitForAck()
    cp.rdData.peekInt()
  }

  def write(addr: Int, data: BigInt) = {
    cp.address.poke(addr.U)
    cp.wrData.poke(data.U)
    cp.wr.poke(true.B)
    cp.rd.poke(false.B)
    cpif.clock.step()
    cp.wr.poke(false.B)
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
