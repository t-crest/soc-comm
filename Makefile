

all: doit test

# Generate Verilog code
doit:
	sbt "runMain empty.AddMain"
	sbt "runMain soc.DirectLink"

# Generate the C++ simulation and run the tests
add-test:
	sbt "test:runMain empty.AddTester"

test-bubble:
	sbt "runMain s4noc.FifoTester"

test:
	sbt "test:runMain s4noc.S4nocTester"

test-all:
	sbt "test:runMain s4noc.ScheduleTester"
	sbt "test:runMain s4noc.RouterTester"
	sbt "test:runMain s4noc.NetworkTester"
	sbt "test:runMain s4noc.NetworkCompare"

# do the right thing, does not yet do it
vcd:
	sbt "testOnly chisel3.tests.BasicTest -- -DwriteVcd=1"

clean:
	git clean -fd
