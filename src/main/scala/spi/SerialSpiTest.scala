package spi

import com.fazecast.jSerialComm._

class SerialSpiTest {

}

object SerialSpiTest extends App {
  val ports = SerialPort.getCommPorts
  for (port <- ports) {
    println(port.getSystemPortName)
    println(port.getDescriptivePortName)
    println(port.getManufacturer)
  }
  // TODO: fix this hard coded thing
  val port = ports(3)
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
  val out = port.getOutputStream
  val buf = new Array[Byte](1)
  for (i <- 0 until 10) {
    out.write('0' + i)
    Thread.sleep(100)
    println(s"Wrote ${'0' + i}")
    if (port.bytesAvailable() != 0) {
      println(s"Bytes available: ${port.bytesAvailable()}")
      val cnt = port.readBytes(buf, 1)
      println("read: " + cnt + " " + buf(0))
    }
  }
  out.close()
  port.closePort()
}
