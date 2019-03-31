SBT = sbt

# Generate Verilog code
doit:
	$(SBT) "runMain empty.AddMain"
	$(SBT) "runMain soc.DirectLink"

# Generate the C++ simulation and run the tests
test:
	$(SBT) "test:runMain empty.AddTester"

clean:
	git clean -fd

