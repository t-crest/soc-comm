package s4noc

import scala.collection.mutable
import scala.util.Random

/**
  * Traffic generator for a NoC.
  * Generates random traffic for a requested injection rate.
  *
  * @param n number of cores (not n x n)
  */
class TrafficGen(n: Int) {

  // one queue per node per destination for ideal measurements
  val queues = Array.ofDim[mutable.Queue[Int]](n, n)
  // one queue per node, including the destination
  val mergedQueues = Array.ofDim[mutable.Queue[(Int, Int)]](n)
  val check = new mutable.HashSet[(Int, Int)]()

  var injectionRate = 0.0
  val r = new Random(0)
  var countCycles = 0
  var inserted = 0

  def reset()  = {
    inserted = 0
    countCycles = 0
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        queues(i)(j) = new mutable.Queue[Int]()
      }
      mergedQueues(i) = new mutable.Queue[(Int, Int)]()
    }
  }

  def insertValue(from: Int, to: Int, data: Int): Unit = {
    queues(from)(to).enqueue(data)
    val v =  (to, data)
    mergedQueues(from).enqueue(v)
    check += v
    // println(s"insert into queues: from $from to $v, check size = ${check.size}")
  }

  def getValue(from: Int, to: Int): Int = {
    val q = queues(from)(to)
    if (q.isEmpty) {
      -1
    } else {
      q.dequeue()
    }
  }

  def getValueForCore(from: Int) = {
    val q = mergedQueues(from)
    if (q.isEmpty) {
      (-1, -1)
    } else {
      q.dequeue()
    }
  }

  reset()

  /**
    * Execute this once per clock cycle and call back for packet injection
    */
  def tick(doInsert: Boolean): Unit = {
    if (doInsert) {
      for (from <- 0 until n) {
        val rnd = r.nextDouble()
        if (rnd <= injectionRate) {
          inserted += 1
          var to = r.nextInt(n)
          while (to == from) {
            to = r.nextInt(n)
          }
          insertValue(from, to, (from << 24) | (to << 16) | countCycles)
        }
      }
    }
    countCycles += 1
  }
}

