package s4noc

import scala.collection.mutable
import scala.util.Random

class TrafficGen(n: Int) {

  val queues = Array.ofDim[mutable.Queue[Int]](n, n)
  for (i <- 0 until n) {
    for (j <- 0 until n) {
      queues(i)(j) = new mutable.Queue[Int]()
    }
  }

  def insert(from: Int, to: Int, at: Int): Unit = {
    queues(from)(to).enqueue(at)
  }

  var countCycles = 0
  var inserted = 0
  var injectionRate = 0.1 * n

  /**
    * Execute this once per clock cycle
    */
  def tick(): Unit = {
    // remember which source and destination was taken in a cycle
    // no doubles
    val fromSet = mutable.Set[Int]()
    val toSet = mutable.Set[Int]()
    val r = new Random()

    def getOne(s: mutable.Set[Int]): Int = {
      var one = r.nextInt(n)
      while (s.contains(one)) {
        one = r.nextInt(n)
      }
      s += one
      one
    }
    countCycles += 1
    while (inserted.toDouble / countCycles < injectionRate) {
      inserted += 1
      val from = getOne(fromSet)
      var to = getOne(toSet)
      if (to == from) to = getOne(toSet)
      println(s"$countCycles $inserted: $from -> $to")
    }
  }
}

object TrafficGen extends App {
  val t = new TrafficGen(8)
  for (i <- 0 until 100) t.tick()
}

