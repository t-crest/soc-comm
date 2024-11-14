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
  val port = ports(3)
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  val out = port.getOutputStream
  for (i <- 0 until 100) {
    out.write('0' + i)
    Thread.sleep(1000)
  }
  out.close()
  port.closePort()
}
