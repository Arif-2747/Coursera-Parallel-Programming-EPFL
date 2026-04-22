package scalashop

import org.scalameter.*

object VerticalBoxBlurRunner:

  val standardConfig = config(
    Key.exec.minWarmupRuns := 5,
    Key.exec.maxWarmupRuns := 10,
    Key.exec.benchRuns := 10,
    Key.verbose := false
  ) withWarmer(Warmer.Default())

  def main(args: Array[String]): Unit =
    val radius = 3
    val width = 1920
    val height = 1080
    val src = Img(width, height)
    val dst = Img(width, height)
    val seqtime = standardConfig measure {
      VerticalBoxBlur.blur(src, dst, 0, width, radius)
    }
    println(s"sequential blur time: $seqtime")

    val numTasks = 32
    val partime = standardConfig measure {
      VerticalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time: $partime")
    println(s"speedup: ${seqtime.value / partime.value}")

object VerticalBoxBlur extends VerticalBoxBlurInterface:

  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit =
    var x = from
    while x < end do
      var y = 0
      while y < src.height do
        dst.update(x, y, boxBlurKernel(src, x, y, radius))
        y += 1
      x += 1

  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit =
    val step = Math.max(1, src.width / numTasks)
    val points = 0 to src.width by step
    val range = if (points.last == src.width) points else points :+ src.width
    val strips = range.zip(range.tail).distinct

    val tasks = strips.map { (f, e) =>
      task {
        blur(src, dst, f, e, radius)
      }
    }
    tasks.foreach(_.join())