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
    Thread.sleep(300)
    ret
  }

  def writeByte(v: Int) = {
    for (i <- 0 until 8) {
      val bits = ((v >> (7-i)) & 1) << 1
      // clock off, set data
      writeRead(s"w44$bits\r")
      // clock on, keep data
      writeRead(s"w44${bits + 1}\r")
    }
    // not now, writeRead("w440\r") // clock off
  }

  def readByte() = {
    var v = 0
    for (i <- 0 until 8) {
      writeRead("w440\r") // clock off
      // data changes after neg edge
      writeRead("w441\r") // clock on
      // sample on pos edge
      val rx = writeRead("r")
      // println("received: " + rx(8))
      // '8' is bit set
      val bit = if (rx(8) == '8') 1 else 0
      v = (v << 1) | bit
    }
    writeRead("w440\r") // clock off (maybe?), does not heart on multibyte read
    v
  }

  def readAdx(cmd: Int): Int = {
    writeRead("w444\r")
    writeRead("w440\r")
    writeByte(cmd)
    writeByte(0)
    val ret = readByte()
    writeRead("w444\r")
    ret
  }

  // TODO: fix this hard coded thing
  val port = SerialPort.getCommPort("/dev/tty.usbserial-210292B408601")
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
  val out = port.getOutputStream

  print(writeRead("w444\r"))
  print(writeRead("w440\r"))
  print(writeRead("r"))
  val v = readAdx(0x0b) // dev id
  println("device id is 0x" + v.toHexString)
  writeRead("w444\r")
  out.close()
  port.closePort()
}
