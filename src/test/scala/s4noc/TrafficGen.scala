package s4noc

import scala.collection.mutable
import scala.util.Random

/**
  *
  * @param n all cores (not n x n)
  */
class TrafficGen(n: Int) {

  val queues = Array.ofDim[mutable.Queue[Int]](n, n)
  for (i <- 0 until n) {
    for (j <- 0 until n) {
      queues(i)(j) = new mutable.Queue[Int]()
    }
  }

  def insert(from: Int, to: Int, data: Int): Unit = {
    queues(from)(to).enqueue(data)
  }

  def getValue(from: Int, to: Int): Int = {
    val q = queues(from)(to)
    if (q.isEmpty) {
      -1
    } else {
      q.dequeue()
    }
  }

  var countCycles = 0
  var inserted = 0
  var injectionRate = 0.5 * n

  // This would be a direct call back into the Chisel tester
  // def tick(inject: (Int, Int) => Unit): Unit = {

  /**
    * Execute this once per clock cycle and call back for packet injection
    */
  def tick(): Unit = {
    // remember which source and destination was taken in a cycle
    // no doubles
    val fromSet = mutable.Set[Int]()
    val toSet = mutable.Set[Int]()
    val r = new Random()

    def getOne(s: mutable.Set[Int]): Int = {
      var one = r.nextInt(n)
      var count = 0
      while (s.contains(one)) {
        one = r.nextInt(n)
        if (count > n) throw new Exception("Livelock in getOne()")
        count += 1
      }
      s += one
      one
    }

    while (inserted.toDouble / (countCycles + 1) < injectionRate) {
      inserted += 1
      val from = getOne(fromSet)
      var to = getOne(toSet)
      if (to == from) to = getOne(toSet)
      println(s"$countCycles $inserted: $from -> $to")
      insert(from, to, (from << 24) | (to << 16) | countCycles)
    }

    countCycles += 1
  }
}

object TrafficGen extends App {

  val n = 4
  val t = new TrafficGen(n)

  for (i <- 0 until 10) {
    t.tick()
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        val data = t.getValue(i, j)
        if (data != -1) {
          println(s"At $data $i -> $j")
        }
      }
    }
  }
}

