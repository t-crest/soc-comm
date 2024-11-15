# This Makefile is outdated
all: doit test

# Generate Verilog code
doit:
	sbt "runMain s4noc.S4NoCVerilogGen 9"

# Run the tests

test:
	sbt "testOnly s4noc.NocTester"

test-all:
	sbt test

perf:
	sbt "Test / run s4noc.PerformanceTest"

# the is an old, for now disabled test
latency:
#	sbt "test / runMain s4noc.MeasureLatency" | grep result

# do the right thing, does not yet do it
vcd:
	sbt "testOnly chisel3.tests.BasicTest -- -DwriteVcd=1"

synpath:
	source /home/shared/Xilinx/Vivado/2017.4/settings64.sh

HW = BitBang

synth:
	./vivado_synth.sh -t $(HW) -p xc7a100tcsg324-1 -x nexysA7.xdc -o build generated/$(HW).v

cp-bit:
	-mkdir build
	scp masca@chipdesign1.compute.dtu.dk:~/t-crest/soc-comm/build/$(HW).bit build
# Configure the Basys3 or NexysA7 board with open source tools
config:
	openocd -f 7series.txt

# serial port on Mac
listen:
	ls /dev/tty.*
	screen /dev/tty.usbserial-210292B408601 115200

# stop with Ctrl+A and Ctrl+\

clean:
	git clean -fd
