# The S4NoC Network-on-Chip

 * Message passing
 * All-to-all communication
 * TDM scheduling - no handshaking 
   - Time-predictable
   - When receiver cannot catch up, messages are dropped
 * Simple memory mapped interface
   - status register (TX empty, RX full)
   - data register (TX data, RX data)
   - destination/source register (TX destination, RX source)
 * See [README of example](https://github.com/t-crest/patmos/tree/master/c/apps/s4noc) for an example

## Publications

```bibtex
@INPR@INPROCEEDINGS{t-crest:s4noc,
author = {Martin Schoeberl and Florian Brandner and Jens Spars{\o} and Evangelia
	Kasapaki},
title = {A Statically Scheduled Time-Division-Multiplexed Network-on-Chip
	for Real-Time Systems},
booktitle = {Proceedings of the 6th International Symposium on Networks-on-Chip
	(NOCS)},
year = {2012},
pages = {152--160},
address = {Lyngby, Denmark},
month = {May},
publisher = {IEEE},
doi = {10.1109/NOCS.2012.25},
url = {http://www.jopdesign.com/doc/s4noc.pdf}
}
```

```bibtex
@InProceedings{s4noc:ni:isorc2024,
  author    = {Martin Schoeberl},
  booktitle = {Proceedings of the 2024 IEEE 27th International Symposium on Real-Time Distributed Computing (ISORC)},
  title     = {Exploration of Network Interface Architectures for a Real-Time Network-on-Chip},
  year      = {2024},
  address   = {United States},
  note      = {2024 IEEE 27th International Symposium on Real-Time Distributed Computing, ISORC ; Conference date: 22-05-2024 Through 25-05-2024},
  publisher = {IEEE},
  doi       = {10.1109/ISORC61049.2024.10551364},
  isbn      = {979-8-3503-7129-1},
}
```

```bibtex

```
## Description

The S4NoC is a message passing network-on-chip (NoC) with an all-to-all
communication pattern. S4NoC uses TDM scheduling to be time-predictable.

The network interface and the S4NOC are written in Chisel and the
source can be found in [s4noc](src/main/scala/s4noc)

The tests can run from the current folder with a plain

```
sbt test
```