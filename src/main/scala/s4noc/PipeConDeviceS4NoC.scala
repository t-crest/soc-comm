package s4noc

import chisel3._

import soc._

class PipeConDeviceS4NoC[T <: Data](private val addrWidth: Int, private val dt: T) extends PipeConDeviceRV(addrWidth, dt, true){

}
