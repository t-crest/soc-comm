package s4noc

import scala.collection.mutable
import scala.util.Random

/**
  *
  * @param n all cores (not n x n)
  */
class TrafficGen(n: Int) {

  val queuesSplit = Array.ofDim[mutable.Queue[Int]](n, n)
  val queues = Array.ofDim[mutable.Queue[(Int, Int)]](n)

  var injectionRate = 0.2 * n
  val r = new Random(0)
  var countCycles = 0
  var inserted = 0

  def reset()  = {
    inserted = 0
    countCycles = 0
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        queuesSplit(i)(j) = new mutable.Queue[Int]()
      }
      queues(i) = new mutable.Queue[(Int, Int)]()
    }
  }


  def insert(from: Int, to: Int, data: Int): Unit = {
    queuesSplit(from)(to).enqueue(data)
    queues(from).enqueue((data, to))
  }

  def getValue(from: Int, to: Int): Int = {
    val q = queuesSplit(from)(to)
    if (q.isEmpty) {
      -1
    } else {
      q.dequeue()
    }
  }

  def getValueFromSingle(from: Int): (Int, Int) = {
    val q = queues(from)
    if (q.isEmpty) {
      (-1, 0)
    } else {
      q.dequeue()
    }
  }

  reset()

  // This would be a direct call back into the Chisel tester
  // def tick(inject: (Int, Int) => Unit): Unit = {

  /**
    * Execute this once per clock cycle and call back for packet injection
    */
  def tick(doInsert: Boolean): Unit = {
    if (doInsert) {
      for (from <- 0 until n) {
        val rnd = r.nextDouble()
        if (rnd < injectionRate) {
          inserted += 1
          var to = r.nextInt(n)
          while (to == from) {
            to = r.nextInt(n)
          }
          insert(from, to, (from << 24) | (to << 16) | countCycles)
        }
      }
    }
    countCycles += 1
  }
}

