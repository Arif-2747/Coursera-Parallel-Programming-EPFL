package scalashop

import java.awt.*
import java.awt.image.*
import java.io.*
import javax.imageio.*
import javax.swing.*

class PhotoCanvas extends JComponent:

  private var imagePath: Option[String] = None
  private var image: Img = loadDefaultImage()

  override def getPreferredSize: Dimension =
    Dimension(image.width, image.height)

  private def loadDefaultImage(): Img =
    val stream = getClass.getResourceAsStream("/scalashop/scala.jpg")
    try loadImage(stream)
    finally stream.close()

  private def loadFromFile(path: String): Img =
    val stream = new FileInputStream(path)
    try loadImage(stream)
    finally stream.close()

  private def loadImage(stream: InputStream): Img =
    val buffered = ImageIO.read(stream)
    val w = buffered.getWidth
    val h = buffered.getHeight
    val img = Img(w, h)
    for
      x <- 0 until w
      y <- 0 until h
    do img(x, y) = buffered.getRGB(x, y)
    img

  def loadFile(path: String): Unit =
    imagePath = Some(path)
    reload()

  def reload(): Unit =
    image = imagePath match
      case Some(path) => loadFromFile(path)
      case None       => loadDefaultImage()
    repaint()

  def applyFilter(filter: String, tasks: Int, radius: Int): Unit =
    val dst = Img(image.width, image.height)
    filter match
      case "horizontal-box-blur" =>
        HorizontalBoxBlur.parBlur(image, dst, tasks, radius)
      case "vertical-box-blur" =>
        VerticalBoxBlur.parBlur(image, dst, tasks, radius)
      case _ =>
    image = dst
    repaint()

  override def paintComponent(g: Graphics): Unit =
    super.paintComponent(g)
    val w = image.width
    val h = image.height
    val buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    for
      x <- 0 until w
      y <- 0 until h
    do buffered.setRGB(x, y, image(x, y))
    g.drawImage(buffered, 0, 0, null)