# The SLimFlit Network-on-Chip

The SLimFlit NoC is a network-on-chip (NoC) with small (single flit)
packets. The router contains only a single pipeline stage and uses.
Single flit packets need no wormhole routing, no virtual channels,
and avoids head-of-line blocking or deadlocks issues.

With traffic shaping and proper scheduling, the SLimFlit NoC can provide
time-predictable communication.