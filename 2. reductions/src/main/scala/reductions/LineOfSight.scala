package reductions

import org.scalameter.*

object LineOfSightRunner:

  val standardConfig = config(
    Key.exec.minWarmupRuns := 40,
    Key.exec.maxWarmupRuns := 80,
    Key.exec.benchRuns := 100,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val length = 10000000
    val input = (0 until length).map(_ % 100 * 1.0f).toArray
    val output = new Array[Float](length + 1)
    val seqtime = standardConfig measure {
      LineOfSight.lineOfSight(input, output)
    }
    println(s"sequential time: $seqtime")
    val partime = standardConfig measure {
      LineOfSight.parLineOfSight(input, output, 10000)
    }
    println(s"parallel time: $partime")
    println(s"speedup: ${seqtime.value / partime.value}")

enum Tree(val maxPrevious: Float):
  case Node(left: Tree, right: Tree) extends Tree(left.maxPrevious.max(right.maxPrevious))
  case Leaf(from: Int, until: Int, override val maxPrevious: Float) extends Tree(maxPrevious)

object LineOfSight extends LineOfSightInterface:

  def lineOfSight(input: Array[Float], output: Array[Float]): Unit =
    var maxAngle = 0f
    var i = 1
    output(0) = 0f
    while i < input.length do
      val angle = input(i) / i
      if angle > maxAngle then maxAngle = angle
      output(i) = maxAngle
      i += 1

  def upsweepSequential(input: Array[Float], from: Int, until: Int): Float =
    var maxAngle = 0f
    var i = from
    while i < until do
      if i > 0 then
        val angle = input(i) / i
        if angle > maxAngle then maxAngle = angle
      i += 1
    maxAngle

  def upsweep(input: Array[Float], from: Int, end: Int, threshold: Int): Tree =
    if end - from <= threshold then
      Tree.Leaf(from, end, upsweepSequential(input, from, end))
    else
      val mid = from + (end - from) / 2
      val (left, right) = parallel(upsweep(input, from, mid, threshold), upsweep(input, mid, end, threshold))
      Tree.Node(left, right)

  def downsweepSequential(input: Array[Float], output: Array[Float],
    startingAngle: Float, from: Int, until: Int): Unit =
    var maxAngle = startingAngle
    var i = from
    while i < until do
      if i > 0 then
        val angle = input(i) / i
        if angle > maxAngle then maxAngle = angle
      output(i) = maxAngle
      i += 1

  def downsweep(input: Array[Float], output: Array[Float], startingAngle: Float, tree: Tree): Unit =
    tree match
      case Tree.Leaf(from, until, _) =>
        downsweepSequential(input, output, startingAngle, from, until)
      case Tree.Node(left, right) =>
        parallel(
          downsweep(input, output, startingAngle, left),
          downsweep(input, output, left.maxPrevious.max(startingAngle), right)
        )

  def parLineOfSight(input: Array[Float], output: Array[Float], threshold: Int): Unit =
    val tree = upsweep(input, 0, input.length, threshold)
    downsweep(input, output, 0f, tree)