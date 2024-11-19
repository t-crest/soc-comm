package spi

import com.fazecast.jSerialComm._

class SerialSpiTest {

}

object SerialSpiTest extends App {

  def writeRead(s: String): String = {
    for (c <- s) {
      out.write(c.toByte)
    }
    // println("Wrote: " + s)
    val buf = new Array[Byte](1)
    var ret = ""
    var stop = false
    while (!stop) {
      if (port.bytesAvailable() > 0) {
        port.readBytes(buf, 1)
        val c = buf(0).toChar
        ret = ret + c
        stop = c == '\n'
      }
    }
    // print("Received: " + ret)
    // Thread.sleep(100)
    ret
  }

  def setCmd(id: Int, v: Int): String = {
    val cmd = v.toHexString
    id match {
      case 0 => "w44" + cmd + "\r"
      case 1 => "w4" + cmd + "4\r"
      case 2 => "w" + cmd + "44\r"
    }
  }

  def writeByte(id: Int, v: Int) = {
    for (i <- 0 until 8) {
      val bits = ((v >> (7-i)) & 1) << 1
      // clock off, set data
      writeRead(setCmd(id, bits)) // clock off, set data
      // clock on, keep data
      writeRead(setCmd(id, bits + 1)) // clock on, keep data
    }
    // not now, writeRead("w440\r") // clock off
  }

  def readByte(id: Int) = {
    var v = 0
    for (i <- 0 until 8) {
      writeRead(setCmd(id, 0)) // clock off
      // data changes after neg edge
      writeRead(setCmd(id, 1)) // clock on
      // sample on pos edge
      val rx = writeRead("r")
      // println("received: " + rx(8))
      // '8' is bit set
      val bit = if (rx(8 - id) == '8') 1 else 0
      v = (v << 1) | bit
    }
    writeRead(setCmd(id, 0)) // clock off (maybe?), does not hurt on multibyte read
    v
  }

  def readAdx(cmd: Int): Int = {
    writeRead(setCmd(0, 4))
    writeRead(setCmd(0, 0)) // CS low
    writeByte(0, cmd)
    writeByte(0, 0)
    val ret = readByte(0)
    writeRead(setCmd(0, 4)) // CS high
    ret
  }

  def readJedecId(id: Int) = {
    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x9f)
    val v = readByte(id)
    println("Manufacturer is 0x" + v.toHexString)
    println("Device type is 0x" + readByte(id).toHexString)
    println("Device id is 0x" + readByte(id).toHexString)
    writeRead(setCmd(id, 4)) // CS high
    v
  }

  def readStatusRegister(id: Int) = {
    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x05)
    val v = readByte(id)
    println("Status register is 0x" + v.toHexString)
    writeRead(setCmd(id, 4)) // CS high
    v
  }

  def readMemory(id: Int, addr: Int) = {
    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x03)
    writeByte(id, (addr >> 16) & 0xff)
    writeByte(id, (addr >> 8) & 0xff)
    writeByte(id, addr & 0xff)
    val v = readByte(id)
    println("Memory of " + id + " at 0x" + addr.toHexString + " is 0x" + v.toHexString)
    writeRead(setCmd(id, 4)) // CS high
    v
  }

  def readSram(id: Int, addr: Int) = {
    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x03)
    writeByte(id, (addr >> 8) & 0xff)
    writeByte(id, addr & 0xff)
    val v = readByte(id)
    println("SRAM of " + id + " at 0x" + addr.toHexString + " is 0x" + v.toHexString)
    writeRead(setCmd(id, 4)) // CS high
    v
  }

  def programFlash(id: Int, addr: Int, data: Array[Byte]) = {

    readStatusRegister(id)

    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x01) // write status register
    writeByte(id, 0x00) // write status register
    writeRead(setCmd(id, 4)) // CS high

    readStatusRegister(id)

    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x06) // write enable
    writeRead(setCmd(id, 4)) // CS high

    readStatusRegister(id)

    writeRead(setCmd(id, 0)) // CS low
    writeByte(id, 0x02)
    writeByte(id, (addr >> 16) & 0xff)
    writeByte(id, (addr >> 8) & 0xff)
    writeByte(id, addr & 0xff)
    for (d <- data) {
      println("Writing 0x" + d.toHexString)
      writeByte(id, d)
    }
    // writeByte(id, 0x04) // write disable
    writeRead(setCmd(id, 4)) // CS high

    readStatusRegister(id)
    Thread.sleep(300)
    readStatusRegister(id)

  }


  // TODO: fix this hard coded thing
  val port = SerialPort.getCommPort("/dev/tty.usbserial-210292B408601")
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
  val out = port.getOutputStream

  print(writeRead(setCmd(0, 4))) // all CS high
  print(writeRead(setCmd(0, 0))) // CS low for ADX
  print(writeRead("r"))
  val v = readAdx(0x0b) // dev id
  println("device id is 0x" + v.toHexString)
  readJedecId(1) // Flash
  readStatusRegister(2) // SRAM
  readMemory(1, 0)
  readSram(2, 0)

  val s = "Hello, World!\n"
  val data = s.getBytes
  // programFlash(1, 0, data)
  Thread.sleep(1000)

  for (i <- 0 until 8) {
    print(readMemory(1, i).toChar)
  }
  println()

  print(writeRead(setCmd(0, 4))) // all CS high
  out.close()
  port.closePort()
}
