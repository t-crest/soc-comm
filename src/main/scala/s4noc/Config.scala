package s4noc

/**
  * S4NoC Configuration case class
  * @param n nodes
  * @param txDepth tx FIFO depth
  * @param rxDepth rx FIFO depth
  * @param width channel width (usually 32)
  */
case class Config(n: Int, txDepth: Int, rxDepth: Int, width: Int = 32) {
  val dim = math.sqrt(n).toInt
  if (dim * dim != n) throw new Error("Number of cores must be quadratic")
}
