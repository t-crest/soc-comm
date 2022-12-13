package wishbone

import chisel3._

/*
Indirection for FlexPRET to WB interface

a read from WB address 42

li x2, 42
sw 0(x0), x2
lw x4, 4(x0)
loop until flag set
lw x4, 3(x0)

// write 123 to address 42
li x2, 42
sw 1(x0), x2
sw 2(x0), 123
lw x4, 4(x0)
loop until flag set

*/

class WishboneIO(addrWith: Int) extends Bundle {
  val addr = Output(UInt(addrWith.W))
  val wrData = Output(UInt(32.W))
  val rdData = Input(UInt(32.W))
  val we = Output(Bool())
  val sel = Output(UInt(4.W))
  val stb = Output(Bool())
  val ack = Input(Bool())
  val cyc = Output(Bool())

  def setDefaults(): Unit = {
    addr := 0.U
    wrData := 0.U
    we := false.B
    cyc := false.B
    sel := 0.U
    stb := 0.U
  }

  def setDefaultsFlipped(): Unit = {
    rdData := 0.U
    ack := false.B
  }  
  
  def driveReadReq(_addr: UInt): Unit = {
    addr := _addr
    wrData := 0.U
    we := false.B
    cyc := true.B
    sel := 15.U// Assume we always want 4 bytes for now
    stb := true.B
  }

  def driveWriteReq(_addr: UInt, data: UInt) = {
    addr := _addr
    wrData := data
    we := true.B
    cyc := true.B
    sel := 15.U // Assume we always want 4 bytes for now
    stb := true.B
  }
}

/*
Notes for slave: reset ack
 */
