package slimflit.arbiter

object BarrelRotater {
  import chisel3._

  def pow2(i: Int): Int = scala.math.pow(2, i).toInt


  def rotatedLeft[T](seq: Seq[T], i: Int): Seq[T] = {
    i match {
      case 0 => seq
      case _ => rotatedLeft(seq.tail :+ seq.head, i - 1)
    }
  }


  def left[T <: Data](in: Seq[T], shamt: UInt): Vec[T] = {
    VecInit(barrelRotateLeft(in, shamt.asBools, 1) { case (d,l,r) => Mux(d,l,r) })
  }

  def barrelRotateLeft[T,D](in: Seq[T], ds: Seq[D], i: Int)(sel: (D,T,T) => T): Seq[T] = {
    ds match {
      case Seq() => in
      case d::_ =>
        val rot = rotatedLeft(in, i)
        val muxed = in.zip(rot).map { case (noShift, shift) => sel(d, shift, noShift) }
        barrelRotateLeft(muxed, ds.tail, i << 1)(sel)
    }
  }

  def rotatedRight[T](seq: Seq[T], i: Int): Seq[T] = {
    i match {
      case 0 => seq
      case _ => rotatedRight(seq.last +: seq.init, i - 1)
    }
  }

  def barrelRotateRight[T,D](in: Seq[T], ds: Seq[D], i: Int)(sel: (D,T,T) => T): Seq[T] = {
    ds match {
      case Seq() => in
      case d::_ =>
        val rot = rotatedRight(in, i)
        val muxed = in.zip(rot).map { case (noShift, shift) => sel(d, shift, noShift) }
        barrelRotateRight(muxed, ds.tail, i << 1)(sel)
    }
  }
  def right[T <: Data](in: Seq[T], shamt: UInt): Vec[T] = {
    VecInit(barrelRotateRight(in, shamt.asBools, 1) { case (d,l,r) => Mux(d,l,r) })
  }
}
