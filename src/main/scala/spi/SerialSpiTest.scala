package spi

import com.fazecast.jSerialComm._

class SerialSpiTest {

}

object SerialSpiTest extends App {
  // TODO: fix this hard coded thing
  val port = SerialPort.getCommPort("/dev/tty.usbserial-210292B408601")
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
  val out = port.getOutputStream
  val buf = new Array[Byte](1)
  val s = "Hello World"
  for (i <- 0 until 10) {
    val v = s(i)
    out.write(v)
    Thread.sleep(100)
    println(s"Wrote ${v.toChar}")
    if (port.bytesAvailable() != 0) {
      println(s"Bytes available: ${port.bytesAvailable()}")
      val cnt = port.readBytes(buf, 1)
      println("read: " + cnt + " " + (buf(0).toChar))
    }
  }
  out.close()
  port.closePort()
}
