package scalashop

import org.scalameter.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

object ScalaShop:

  class AppFrame extends JFrame("ScalaShop\u2122"):

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setSize(1024, 600)
    setLayout(BorderLayout())

    private val canvas = PhotoCanvas()

    private val sidePanel = JPanel(BorderLayout())
    sidePanel.setBorder(BorderFactory.createEtchedBorder())
    add(sidePanel, BorderLayout.EAST)

    private val controls = JPanel(GridLayout(0, 2))
    sidePanel.add(controls, BorderLayout.NORTH)

    private val filterBox = JComboBox(Array(
      "horizontal-box-blur",
      "vertical-box-blur"
    ))

    private val radiusSpin = JSpinner(SpinnerNumberModel(3, 1, 16, 1))
    private val taskSpin = JSpinner(SpinnerNumberModel(32, 1, 128, 1))

    controls.add(JLabel("Filter"))
    controls.add(filterBox)

    controls.add(JLabel("Radius"))
    controls.add(radiusSpin)

    controls.add(JLabel("Tasks"))
    controls.add(taskSpin)

    private val applyBtn = JButton("Apply filter")
    applyBtn.addActionListener((_: ActionEvent) =>
      val t = measure {
        canvas.applyFilter(currentFilter, taskCount, radiusValue)
      }
      updateInfo(t.value)
    )
    controls.add(applyBtn)

    private val reloadBtn = JButton("Reload")
    reloadBtn.addActionListener((_: ActionEvent) =>
      canvas.reload()
    )
    controls.add(reloadBtn)

    private val infoBox = JTextArea(" ")
    infoBox.setBorder(BorderFactory.createLoweredBevelBorder())
    sidePanel.add(infoBox, BorderLayout.SOUTH)

    private val menuBar = JMenuBar()

    private val fileMenu = JMenu("File")
    val openItem = JMenuItem("Open...")
    openItem.addActionListener((_: ActionEvent) =>
      val chooser = JFileChooser()
      if chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION then
        canvas.loadFile(chooser.getSelectedFile.getPath)
    )

    val exitItem = JMenuItem("Exit")
    exitItem.addActionListener((_: ActionEvent) => sys.exit(0))

    fileMenu.add(openItem)
    fileMenu.add(exitItem)

    private val helpMenu = JMenu("Help")
    val aboutItem = JMenuItem("About")
    aboutItem.addActionListener((_: ActionEvent) =>
      JOptionPane.showMessageDialog(
        this,
        "ScalaShop, the ultimate image manipulation tool\nEPFL"
      )
    )
    helpMenu.add(aboutItem)

    menuBar.add(fileMenu)
    menuBar.add(helpMenu)
    setJMenuBar(menuBar)

    add(JScrollPane(canvas), BorderLayout.CENTER)

    setVisible(true)

    private def updateInfo(time: Double): Unit =
      infoBox.setText(s"Time: $time")

    private def taskCount: Int =
      taskSpin.getValue.asInstanceOf[Int]

    private def radiusValue: Int =
      radiusSpin.getValue.asInstanceOf[Int]

    private def currentFilter: String =
      filterBox.getSelectedItem.asInstanceOf[String]

  try
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  catch
    case _: Exception => ()

  private val frame = AppFrame()

  def main(args: Array[String]): Unit =
    frame.repaint()