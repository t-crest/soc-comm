package debug

import chisel.lib.uart._
import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

/**
  * Poor mans debugger, using a UART instead of JTAG.
  */
class UartDebug(frequ: Int, baudRate: Int = 115200) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val dout = Output(UInt(32.W))
    val din = Input(UInt(32.W))
  })

  // ASCII -> hex including valid signal
  def ascii2hex(in: UInt) = {
    val out = Wire(UInt(8.W))
    out := 16.U
    switch(in) {
      is('0'.U) { out := "h0".U }
      is('1'.U) { out := "h1".U }
      is('2'.U) { out := "h2".U }
      is('3'.U) { out := "h3".U }
      is('4'.U) { out := "h4".U }
      is('5'.U) { out := "h5".U }
      is('6'.U) { out := "h6".U }
      is('7'.U) { out := "h7".U }
      is('8'.U) { out := "h8".U }
      is('9'.U) { out := "h9".U }
      is('a'.U) { out := "ha".U }
      is('b'.U) { out := "hb".U }
      is('c'.U) { out := "hc".U }
      is('d'.U) { out := "hd".U }
      is('e'.U) { out := "he".U }
      is('f'.U) { out := "hf".U }
      is('A'.U) { out := "hA".U }
      is('B'.U) { out := "hB".U }
      is('C'.U) { out := "hC".U }
      is('D'.U) { out := "hD".U }
      is('E'.U) { out := "hE".U }
      is('F'.U) { out := "hF".U }
    }
    (out(3, 0), out =/= 16.U)
  }

  def hex2ascii(in: UInt) = {
    val out = Wire(UInt(8.W))
    out := 0.U
    switch(in) {
      is("h0".U) { out := '0'.U }
      is("h1".U) { out := '1'.U }
      is("h2".U) { out := '2'.U }
      is("h3".U) { out := '3'.U }
      is("h4".U) { out := '4'.U }
      is("h5".U) { out := '5'.U }
      is("h6".U) { out := '6'.U }
      is("h7".U) { out := '7'.U }
      is("h8".U) { out := '8'.U }
      is("h9".U) { out := '9'.U }
      is("ha".U) { out := 'a'.U }
      is("hb".U) { out := 'b'.U }
      is("hc".U) { out := 'c'.U }
      is("hd".U) { out := 'd'.U }
      is("he".U) { out := 'e'.U }
      is("hf".U) { out := 'f'.U }
    }
    out
  }

  val tx = Module(new BufferedTx(100000000, baudRate))
  val rx = Module(new Rx(100000000, baudRate))

  io.tx := tx.io.txd
  rx.io.rxd := io.rx

  val inReg = RegInit(0.U(8.W))
  val validReg = RegInit(false.B)

  rx.io.channel.ready := true.B
  when (rx.io.channel.valid) {
    inReg := rx.io.channel.bits
    validReg := true.B
  }

  tx.io.channel.bits := inReg
  tx.io.channel.valid := false.B

  when (tx.io.channel.ready && validReg) {
    validReg := false.B
    tx.io.channel.valid := true.B
  }

  val doutReg = RegInit(0x0.U(32.W))
  val doutShftReg = RegInit(0.U(32.W))
  val dinShftReg = RegInit(0.U(32.W))
  val cntRdReg = RegInit(0.U(4.W))

  object State extends ChiselEnum {
    val sIdle, sLineFeed, sWrite, sRead, sCr = Value
  }
  import State._
  val stateReg = RegInit(sIdle)

  switch(stateReg) {
    is(sIdle) {
      when (validReg && inReg === 'w'.U) {
        stateReg := sWrite
        doutShftReg := 0.U
      }
      when (validReg && inReg === 'r'.U) {
        stateReg := sRead
        dinShftReg := io.din
        cntRdReg := 8.U
      }
    }
    is(sWrite) {
      when (validReg) {
        when (inReg === '\r'.U) {
          stateReg := sLineFeed
          doutReg := doutShftReg
        }
        val (out, valid) = ascii2hex(inReg)
        when (valid) {
          doutShftReg := doutShftReg(31, 8)
          doutShftReg := doutShftReg ## out
        }
      }
    }
    is(sLineFeed) {
      tx.io.channel.valid := true.B
      tx.io.channel.bits := '\n'.U
      when (tx.io.channel.ready) {
        stateReg := sIdle
      }
    }
    is(sRead) {
      when (cntRdReg =/= 0.U) {
        tx.io.channel.valid := true.B
        tx.io.channel.bits := hex2ascii(dinShftReg(31, 28))
        when (tx.io.channel.ready) {
          dinShftReg := dinShftReg << 4
          cntRdReg := cntRdReg - 1.U
        }
      }
      when (cntRdReg === 0.U) {
        tx.io.channel.valid := true.B
        tx.io.channel.bits := '\r'.U
        when (tx.io.channel.ready) {
          stateReg := sLineFeed
        }
      }
    }
  }
  io.dout := doutReg
}
