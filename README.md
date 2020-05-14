# Multicore Communication

Collection of different communication methods for chip mulitprocessors.

This repo shall include all the work we have done in T-CREST as a
standalone repo to make the work more useful.

Currently we plan to use a simple rd/wr/address/data/rdy interface, which maps
dirctly to the Patmos OCPcore interface (A command is acked in the next
clock cycle or later, IO devices need to be ready to accept a command).

The S4NOC has a slightly different interface (no rdy needed).

We may consider to move to AXI.

## Dependency

This project depends on ```ip-contributions```. Initialize it with:

```
make init
```

## Setup

The hardware is described in [Chisel](https://chisel.eecs.berkeley.edu/)
and needs just a Java JDK (version 8) and `sbt` installed. All other needed packages
will be automatically downloaded by `sbt`.


On a Mac with OS X `sbt` can be installed, assuming using [homebrew](http://brew.sh/)
as your package management tool, with:
```
brew install sbt
```

On a Debian based Linux machine, such as Ubuntu, you can install `sbt` with:
```
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt
```

For the Chisel based tests compiler with gcc like interface is needed.

## Projects

### S4NOC

The network interface and the S4NOC are written in Chisel and the
source can be found at: `patmos/hardware/src/main/scala/s4noc`.

The tests can run from within folder `patmos/hardware`, e.g.:

	sbt "test:runMain s4noc.ScheduleTester"
	sbt "test:runMain s4noc.RouterTester"
	sbt "test:runMain s4noc.NetworkTester"
	sbt "test:runMain s4noc.NetworkCompare"
	sbt "test:runMain s4noc.S4nocTester"

or from your favorite Scala IDE (e.g., InelliJ or Eclipse) or from this folder with

```bash
make test-all
make test
```

A standalone version of the S4NoC with simple traffic generators can be built
with:

```bash
sbt "runMain s4noc.S4nocTrafficGen n"
```

where n is the number of cores (e.g., 4, 9, or 16 (maximum is 100)).

The generated Verilog file can be found in ```generated/S4nocTrafficGen.v```
and can be synthesized to provide resource numbers and maximum
clocking frequency. An example project for Quartus can be found in this
[quartus](quartus) subfolder.

## TODO

 * Move all tests to ScalaTest, drop iotester dependency, and fix README.md
 * Have the core # to slot # mapping in HW (it should be cheap)
 * Get the Chisel 2 NoC running
 * Have a better defined CPU interface, with one cycle latency here not in OCP wrapper
 * Provide blocking in the CPU interface if not data available or TF full

### Next Paper

 * Build a standard NoC router for best effort