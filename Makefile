

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

clean:
	git clean -fd
