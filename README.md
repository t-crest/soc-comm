# Multicore Communication

Collection of different communication methods for chip mulitprocessors.

This repo shall include all the work we have done in T-CREST as a
standalone repo to make the work more useful.

Currently we use a simple rd/wr/address/data/rdy interface, which maps
dirctly to the Patmos OCPcore interface (A command is acked in the next
clock cycle or later, IO devices need to be ready to accept a command).

We may consider to move to AXI.
