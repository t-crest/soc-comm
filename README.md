![build status](https://github.com/t-crest/soc-comm/actions/workflows/scala.yml/badge.svg)

# Multicore Communication

Collection of different communication methods for chip mulitprocessors.

This repo shall include all the multicore communication we have done in
[T-CREST](https://github.com/t-crest) as a standalone repo to make the
work more useful.

Currently we plan to use a simple rd/wr/address/data/rdy interface, which maps
dirctly to the Patmos OCPcore interface (A command is acked in the next
clock cycle or later, IO devices need to be ready to accept a command
during the ack cycle).

The S4NOC has currently a slightly different interface (no rdy needed).

We may consider moving to AXI. Or better provide a bridge, as AXI is not so super nice.

## Usage

This project is published with Maven Central. Add following line to your ```build.sbt```

```
libraryDependencies += "io.github.t-crest" % "soc-comm" % "0.1.4"
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
name PipeCon for pipelined connection.
The interface consisting of following signals:

```scala
class PipeConIO(private val addrWidth: Int) extends Bundle {
   val address = Output(UInt(addrWidth.W))
   val rd = Output(Bool())
   val wr = Output(Bool())
   val rdData = Input(UInt(32.W))
   val wrData = Output(UInt(32.W))
   val wrMask = Output(UInt(4.W))
   val ack = Input(Bool())
}
```

The main rules define PipeCon:

 * There are two transactions: read and write
 * The transaction command is valid for a single clock cycle
 * The IO device responds earliest in the following clock cycle with an asserted `ack` signal
 * A read result is valid in the clock cycle `ack` is asserted
 * An IO device can insert wait cycles by asserting `ack` later
 * The CPU may issue a new read or write command in the same cycle `ack` is asserted

The PipeCon specification fits well with...

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

### S4NOC

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
sbt "test:runMain s4noc.PerformanceTest"
```

#### TODO (S4NOC)

 * Re-enable the second test in PerformanceTest
 * Major change now (partially DONE): use core id in the NI, the FIFOs, and the testers. CPU interface should be natural
   * 0 split buffer should probably be just a single register, is this different from a single element BubbleFifo?
   * NetworkInterfaceSingle needs to be changed
   * Should also check how much HW this is, probably nothing. Max 4 LUTs for a table for 16 cores
 * Find a way to configure
 * NI should not use split buffers when size is 0
 * Find a way to receive all inserted packets, this might improve the numbers
   * Some single packets are still left over from one run to the next
   * Maybe this is fixed now after the FIFO fixes?
 * Have the core # to slot # mapping in HW (it should be cheap)
   * Then change the (FIFO) buffers to include the destination instead of the time slot
   * Is this already done?
 * Play with FIFO buffer variations

To analyze memory issues (o increase the heap size with Xmx) use a ```.sbtopts``` with
```
-J-XX:+HeapDumpOnOutOfMemoryError
-J-XX:HeapDumpPath=.
-J-Xmx4G
```

## TODO

 * Use and document the PipeCon, direction from master
 * Integrate a simple multicore device with T-CREST
   * A multicore "Hello World"
 * Run S4NOC with T-CREST
 * Move (copy) the fair arbiter from the Chisel book into this repo
 * Do traffic generators like in the Dally book
 * Provide blocking in the CPU interface if not data available or TX FIFO full
   * There is one in the Chisel book
 * Get one-way memory back in here
 * Wishbone wrapper
 * AXI wrapper

### Next Paper

 * Build a standard NoC router for best effort
