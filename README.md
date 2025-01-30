![build status](https://github.com/t-crest/soc-comm/actions/workflows/scala.yml/badge.svg)

# Multicore Communication

Collection of different communication methods for chip mulitprocessors.

This repo shall include all the multicore communication we have done in
[T-CREST](https://github.com/t-crest) as a standalone repo to make the work more useful.

We use a simple rd/wr/address/data/rdy interface, which maps
directly to the Patmos OCPcore interface (A command is acked in the next
clock cycle or later, IO devices need to be ready to accept a command
during the ack cycle).

The repo also contains a Wishbone wrapper.

We plan to  provide a bridge to AXI, as AXI is not so super nice.

## Usage

This project is published with Maven Central. Add following line to your ```build.sbt```

```
libraryDependencies += "io.github.t-crest" % "soc-comm" % "0.1.5"
```
## Dependency

This project depends on [ip-contributions](https://github.com/freechipsproject/ip-contributions),
which is resolved in ```build.sbt``` 

## Setup

The hardware is described in [Chisel](https://chisel.eecs.berkeley.edu/)
and needs just a Java JDK (version 8 or 11) and `sbt` installed. All other needed packages
will be automatically downloaded by `sbt`.


On a Mac with OS X `sbt` can be installed, assuming using [homebrew](http://brew.sh/)
as your package management tool, with:
```
brew install sbt
```

On a Linux machine, install `sbt` according to the instructions from [sbt download](https://www.scala-sbt.org/download.html)

For the Chisel based tests a compiler with gcc like interface is needed.

## Projects

### The CPU Interface PipeCon

For this project we define a simple pipelined IO interface, that we
name `PipeCon` for pipelined connection.
The interface consisting of following signals:

```scala
class PipeCon(private val addrWidth: Int) extends Bundle {
   val address = Input(UInt(addrWidth.W))
   val rd = Input(Bool())
   val wr = Input(Bool())
   val rdData = Output(UInt(32.W))
   val wrData = Input(UInt(32.W))
   val wrMask = Input(UInt(4.W))
   val ack = Output(Bool())
}
```

```PipeCon``` itself is an abstract class, just containing the interface:

```scala
abstract class PipeConDevice(addrWidth: Int) extends Module {
   val cpuPort = IO(new PipeConIO(addrWidth))
}
```

The main rules that define PipeCon:

 * There are two transactions: read and write
 * The transaction command is valid for a single clock cycle
 * The IO device responds earliest in the following clock cycle with an asserted `ack` signal
 * A read result is valid in the clock cycle `ack` is asserted
 * An IO device can insert wait cycles by asserting `ack` later
 * The CPU may issue a new read or write command in the same cycle `ack` is asserted

The PipeCon specification fits well for pipelined processors,
being parallel to the memory stage that has one clock cycle read
latency.

This definition is basically the same as the CoreIO from Patmos,
which itself is a valid OCP interface. However, as OCP is a big specification
and not used so much, we define here the simplified version without
a reference to OCP.

A read or write command are signaled by an asserted ```rd``` or ```wr```.
The address and write data (if it is a write) need to be valid during
the command. Commands are only valid for a single cycle.
Each command needs to be acknowledged by an active ```ack```,
from the IO device earliest one cycle after the command.
The IO device can also insert wait
states by delaying ```ack```. Read data is available with the ```ack```
signal for one clock cycle.

![handshake](handshake.svg)

The figure shows such a bus protocol that does not need
a combinational reaction of the peripheral device.
It is pipelined handshaking, as we propose it in this project.
The request from the processor  is only a single cycle long.
The address bus and the read signal does not need to be driven
till the acknowledgment. The ```ack``` signal comes earliest
one clock cycle after the ```rd``` command, in clock cycle 3.
The first read sequence has one cycle latency in this example.
the same latency as the former example.
However, as the request needs to be valid only one clock cycle,
we can pipeline requests.
Read of addresses ```A2``` and ```A3``` can be requested back to back,
allowing a throughput of 1 data word per clock cycle.

The Patmos processor uses an OCP version with exact this
protocol for accessing IO devices (OCPcore). Memory is connected via a burst interface.
The Patmos Handbook gives a detailed description of the
used OCP interfaces.

#### Ready/Valid Interface

For IO devices with a ready/valid interface (Chisel ```Decoupled```) we
provide a standard mapping for the ```PipeCon```, the ```PipeConRV```:

 * CPU interface to two ready/valid channels (one for transmit/tx, one for receive/rx).
 * IO mapping as in the classic PC serial port (UART)
 * 0: status (control): bit 0 tx ready, bit 1 rx data available
 * 4: write into txd and read from rxd

Additionally, for the S4NOC we provide following port:

 * 8: write destination, read source (S4NOC specific)



### S4NOC

See more details in [S4NoC.md](S4NoC.md)

The network interface and the S4NOC are written in Chisel and the
source can be found in [s4noc](src/main/scala/s4noc)

The tests can run from the current folder with a plain

```
sbt test
```


or from your favorite Scala IDE (e.g., InelliJ or Eclipse).

To generate the Verilog code with a traffic generator execute

```
sbt "runMain s4noc.S4nocTrafficGen n"
```

where `n` is the number of cores.

The generated Verilog file can be found in ```generated/S4nocTrafficGen.v```
and can be synthesized to provide resource numbers and maximum
clocking frequency. An Intel Qartus project is available in [quartus](quartus).

The performance test is run as an application within the test folder:

```
sbt "Test / run s4noc.PerformanceTest"
```

#### TODO (S4NOC)

 * NetworkTest and LatencyTest disabled, as they (now) run too long
 * Share testing code between ideal and concrete NIs
 * Play with configuration
 * Check memory FIFO if it is memory in an FPGA
 * Should also check how much HW the translation is, probably nothing. Max 4 LUTs for a table for 16 cores
 * Play with FIFO buffer variations
 * Have Raw tester with Verilator annotation

To analyze memory issues (e.g., increase the heap size with Xmx) use a ```.sbtopts``` with
```
-J-XX:+HeapDumpOnOutOfMemoryError
-J-XX:HeapDumpPath=.
-J-Xmx4G
```

## TODO

 * [x] Use and document the PipeCon, direction from slave
 * [x] Use a better name for the PipeCon interface (not io)
 * [x] Wrapper for OCP (in Patmos)
 * [ ] Integrate a simple multicore device with T-CREST
   * A multicore "Hello World" also for the handbook
 * [*] Run S4NOC with T-CREST
 * [ ] Move (copy) the fair arbiter from the Chisel book into this repo
   * [ ] Write a test for the arbiter (or delegate it)
 * [ ] Use that arbiter for access to the serial port in T-CREST (using the ip-contributions version)
 * [ ] Do traffic generators like in the Dally book
 * [ ] Provide blocking in the CPU interface if not data available or TX FIFO full
   * [x] DONE
   * [ ] There is one in the Chisel book, compare them, maybe make them the same
 * [ ] Get one-way memory back in here
 * [ ] AXI wrapper

OCP Wrapper like this (we have a different one, should check the difference)

```scala
class S4nocOCPWrapper(nrCores: Int, txFifo: Int, rxFifo: Int) extends CmpDevice(nrCores) {

  val s4noc = Module(new S4noc(nrCores, txFifo, rxFifo))

  for (i <- 0 until nrCores) {

    val resp = Mux(io.cores(i).M.Cmd === OcpCmd.RD || io.cores(i).M.Cmd === OcpCmd.WR,
      OcpResp.DVA, OcpResp.NULL)

    // addresses are in words
    s4noc.io.cpuPorts(i).addr := io.cores(i).M.Addr
    s4noc.io.cpuPorts(i).wrData := io.cores(i).M.Data
    s4noc.io.cpuPorts(i).wr := io.cores(i).M.Cmd === OcpCmd.WR
    s4noc.io.cpuPorts(i).rd := io.cores(i).M.Cmd === OcpCmd.RD
    io.cores(i).S.Data := RegNext(s4noc.io.cpuPorts(i).rdData)
    io.cores(i).S.Resp := Reg(init = OcpResp.NULL, next = resp)
  }
}
```

### Next Paper

 * Build a standard NoC router for best effort
