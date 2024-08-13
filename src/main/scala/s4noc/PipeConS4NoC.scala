package s4noc

import chisel3._

import soc._

class PipeConS4NoC[T <: Data](private val addrWidth: Int, private val dt: T) extends PipeConRV(addrWidth, dt, true){

}
