package reductions

import scala.annotation.*
import org.scalameter.*

object ParallelParenthesesBalancingRunner:

  @volatile var seqResult = false
  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns := 40,
    Key.exec.maxWarmupRuns := 80,
    Key.exec.benchRuns := 120,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime")
    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime")
    println(s"speedup: ${seqtime.value / fjtime.value}")

object ParallelParenthesesBalancing extends ParallelParenthesesBalancingInterface:

  def balance(chars: Array[Char]): Boolean =
    var count = 0
    var i = 0
    while i < chars.length do
      if chars(i) == '(' then count += 1
      else if chars(i) == ')' then
        count -= 1
        if count < 0 then return false
      i += 1
    count == 0

  def parBalance(chars: Array[Char], threshold: Int): Boolean =

    def traverse(idx: Int, until: Int, open: Int, close: Int): (Int, Int) =
      if idx >= until then (open, close)
      else if chars(idx) == '(' then traverse(idx + 1, until, open + 1, close)
      else if chars(idx) == ')' then
        if open > 0 then traverse(idx + 1, until, open - 1, close)
        else traverse(idx + 1, until, open, close + 1)
      else traverse(idx + 1, until, open, close)

    def reduce(from: Int, until: Int): (Int, Int) =
      if until - from <= threshold then traverse(from, until, 0, 0)
      else
        val mid = from + (until - from) / 2
        val ((lo, lc), (ro, rc)) = parallel(reduce(from, mid), reduce(mid, until))
        val matched = lo.min(rc)
        (lo - matched + ro, lc + rc - matched)

    reduce(0, chars.length) == (0, 0)