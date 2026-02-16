# The SlimFlit Network-on-Chip

SlimFlit is a network-on-chip (NoC) with a small (single flit)
packets. The router contains only a single pipeline stage.
Single flit packets need no wormhole routing, no virtual channels,
and avoid head-of-line blocking or deadlock issues.

With traffic shaping and proper scheduling, the SlimFlit NoC can provide
time-predictable communication.

Have some TODO, e.g., arbitration for output ports,...