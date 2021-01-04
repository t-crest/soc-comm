
all: doit test

# Generate Verilog code
doit:
	sbt "runMain empty.AddMain"
	sbt "runMain soc.DirectLink"

# Run the tests
add-test:
	sbt "test:runMain empty.AddTester"

test:
	sbt "testOnly s4noc.NocTester"

test-all:
	sbt test

latency:
	sbt "test:runMain s4noc.MeasureLatency" | grep result

# do the right thing, does not yet do it
vcd:
	sbt "testOnly chisel3.tests.BasicTest -- -DwriteVcd=1"

clean:
	git clean -fd
