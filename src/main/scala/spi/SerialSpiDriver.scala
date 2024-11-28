package spi

import com.fazecast.jSerialComm._

import chiseltest._

/**
 * A simple driver for SPI communication via the serial port and the debug interface.
 * USed from this App, the FlashTest with testing the controller, and should be used by Wildcat in simulation.
 * @param id (Adx, Flash, SRAM)
 */
class SerialSpiDriver(id: Int, portName: String = "/dev/tty.usbserial-210292B408601") {

  // TODO: fix the hard coded port name
  val port = SerialPort.getCommPort(portName)
  port.openPort()
  port.setBaudRate(115200)
  // port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
  port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
  val out = port.getOutputStream
  csHigh()

  def writeReadSerial(s: String): String = {
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

    // Thread.sleep(10)
    ret
  }

  def setCmd(v: Int): String = {
    val cmd = v.toHexString
    id match {
      case 0 => "w44" + cmd + "\r"
      case 1 => "w4" + cmd + "4\r"
      case 2 => "w" + cmd + "44\r"
    }
  }

  def writeByte(v: Int) = {
    for (i <- 0 until 8) {
      val bits = ((v >> (7-i)) & 1) << 1
      // clock off, set data
      writeReadSerial(setCmd(bits)) // clock off, set data
      // clock on, keep data
      writeReadSerial(setCmd(bits + 1)) // clock on, keep data
    }
    // not now, writeRead("w440\r") // clock off
  }

  def readByte() = {
    var v = 0
    for (i <- 0 until 8) {
      writeReadSerial(setCmd(0)) // clock off
      // data changes after neg edge
      writeReadSerial(setCmd(1)) // clock on
      // sample on pos edge
      val rx = writeReadSerial("r")
      // '8' is MISO bit set
      val bit = if (rx(8 - id) == '8') 1 else 0
      v = (v << 1) | bit
    }
    writeReadSerial(setCmd(0)) // clock off (maybe?), does not hurt on multibyte read
    v
  }

  def csLow() = writeReadSerial(setCmd(0))

  def csHigh() = writeReadSerial(setCmd(4))

  def readAdx(cmd: Int): Int = {
    csHigh()
    csLow()
    writeByte(cmd)
    writeByte(0)
    val ret = readByte()
    csHigh()
    ret
  }

  def readJedecId() = {
    csLow()
    writeByte(0x9f)
    val v = readByte()
    println("Manufacturer is 0x" + v.toHexString)
    println("Device type is 0x" + readByte().toHexString)
    println("Device id is 0x" + readByte().toHexString)
    csHigh()
    v
  }

  // The SRAM needs a delay after the command
  def readJedecIdWait() = {
    csLow()
    writeByte(0x9f)
    writeByte(0)
    writeByte(0)
    writeByte(0)
    val v = readByte()
    println("Manufacturer is 0x" + v.toHexString)
    println("Device type is 0x" + readByte().toHexString)
    println("Device id is 0x" + readByte().toHexString)
    csHigh()
    v
  }

  def readStatusRegister() = {
    csLow()
    writeByte(0x05)
    val v = readByte()
    println("Status register is 0x" + v.toHexString)
    csHigh()
    v
  }

  def readMemory(addr: Int) = {

    println("Reading Memory of " + id + " at 0x" + addr.toHexString)
    csLow()
    writeByte(0x03)
    writeByte((addr >> 16) & 0xff)
    writeByte((addr >> 8) & 0xff)
    writeByte(addr & 0xff)
    val v = readByte()
    println("Memory of " + id + " at 0x" + addr.toHexString + " is 0x" + v.toHexString)
    csHigh()
    v
  }

  def readMemory(addr: Int, buf: Array[Byte]) = {

    println("Reading Memory of " + id + " from 0x" + addr.toHexString)
    csLow()
    writeByte(0x03)
    writeByte((addr >> 16) & 0xff)
    writeByte((addr >> 8) & 0xff)
    writeByte(addr & 0xff)
    for (i <- 0 until buf.length) {
      buf(i) = readByte().toByte
    }
    csHigh()
  }

  // TODO: is this really the way to do it? Anas hae a different solution, sending  3 bytes of address.
  def readSram(addr: Int) = {
    csLow()
    writeByte(0x03)
    writeByte((addr >> 16) & 0xff)
    writeByte((addr >> 8) & 0xff)
    writeByte(addr & 0xff)
    val v = readByte()
    println("SRAM of " + id + " at 0x" + addr.toHexString + " is 0x" + v.toHexString)
    csHigh()
    v
  }

  def writeSram(addr: Int, data: Int) = {
    csLow()
    writeByte(0x02)
    writeByte((addr >> 16) & 0xff)
    writeByte((addr >> 8) & 0xff)
    writeByte(addr & 0xff)
    writeByte(data)
    csHigh()
  }

  def writeCmd(v: Int) = {
    csLow()
    writeByte(v)
    csHigh()
  }
  def writeCmd(v: Int, d: Int) = {
    csLow()
    writeByte(v)
    writeByte(d)
    csHigh()
  }

  def eraseFlash() = {

    println("Erasing Flash")
    writeCmd(0x06) // write enable
    writeCmd(0x01, 0x00) // write 0 into status register
    writeCmd(0x06) // write enable
    readStatusRegister()
    writeCmd(0x60) // chip erase
    readStatusRegister()
    Thread.sleep(1000)
    readStatusRegister()
  }
  def programFlash(id: Int, addr: Int, data: Array[Byte]) = {

    println("Programming Flash at 0x" + addr.toHexString)
    readStatusRegister()
    writeCmd(0x06) // write enable
    readStatusRegister()
    writeCmd(0x01, 0x00) // write 0 into status register
    readStatusRegister()
    writeCmd(0x06) // write enable
    readStatusRegister()

    csLow()
    writeByte(0x02)
    writeByte((addr >> 16) & 0xff)
    writeByte((addr >> 8) & 0xff)
    writeByte(addr & 0xff)
    for (d <- data) {
      println("Writing 0x" + d.toHexString)
      writeByte(d)
    }
    csHigh()

    writeCmd(0x04) // write disable

    readStatusRegister()
    Thread.sleep(300)
    readStatusRegister()
  }

  /**
   * Watch SPI output pins in simulation and forward them via the serial port debugger to the real Flash.
   * And the other way around, read the MISO pin from the Flash and forward it to the SPI input pin in simulation.
   * @param spi
   */
  def echoPins(spi: SpiIO) = {
    val sck = spi.sclk.peekInt().toInt
    val mosi = spi.mosi.peekInt().toInt
    val ncs = spi.ncs.peekInt().toInt
    val bits = (ncs << 2) | (mosi << 1) | sck
    val s = "w4" + (bits + '0').toChar + "4\r"
    writeReadSerial(s)
    val rx = writeReadSerial("r")
    // '8' is MISO bit set
    val bit = if (rx(8 - 1) == '8') 1 else 0
    spi.miso.poke(bit)
  }
}

/**
 * Test SPI components and program the Flash.
 */
object SerialSpiDriver extends App {

  val spi = new SerialSpiDriver(1) // Flash

  spi.csLow()
  print(spi.writeReadSerial("r"))
  spi.csHigh()
  // val v = spi.readAdx(0x0b) // dev id
  // println("device id is 0x" + v.toHexString)
  spi.readJedecId() // Flash
  spi.readStatusRegister()
  spi.readMemory(0)

  // spi.readSram(0)

  // spi.eraseFlash()
  val s = "Hello Wildcat!\n"
  val data = s.getBytes
  // spi.programFlash(1, 0, data)
  // Thread.sleep(1000)
  val prog = Array(0x11100093, 0x22200113, 0x002081b3)
  val by = new Array[Byte](12)
  for (i <- 0 until prog.length) {
    for (j <- 0 until 4) {
      val b = prog(i) >> (8 * j) & 0xff
      println(f"$b%02x")
      by(i * 4 + j) = b.toByte
    }
  }
  // spi.programFlash(1, 0, by)

  val buf = new Array[Byte](20)
  spi.readMemory(0, buf)
  println(new String(buf))
  /*
  for (i <- 0 until 20) {
    print(spi.readMemory(i).toChar)
  }
  println()
   */
  print(spi.writeReadSerial(spi.setCmd(4))) // all CS high
  spi.out.close()
  spi.port.closePort()


  println("SRAM test")
  val sram = new SerialSpiDriver(2) // SRAM
  sram.readJedecIdWait()
  sram.readMemory(0)
  sram.writeSram(0, 0x55)
  sram.readMemory(0)
  sram.writeSram(0, 0xaa)
  sram.readMemory(0)
}
