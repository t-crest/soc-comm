# This Makefile is outdated
all: doit test

# Generate Verilog code
doit:
	sbt "runMain s4noc.S4nocTrafficGen 9"

# Run the tests

test:
	sbt "testOnly s4noc.NocTester"

test-all:
	sbt test

perf:
	sbt "test:run s4noc.PerformanceTest"

latency:
	sbt "test:runMain s4noc.MeasureLatency" | grep result

# do the right thing, does not yet do it
vcd:
	sbt "testOnly chisel3.tests.BasicTest -- -DwriteVcd=1"

clean:
	git clean -fd
