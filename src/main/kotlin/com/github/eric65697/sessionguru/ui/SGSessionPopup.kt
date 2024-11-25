package com.github.eric65697.sessionguru.ui

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.ui.WindowRoundedCornersManager
import com.intellij.ui.components.JBList
import com.intellij.ui.popup.PopupUpdateProcessor
import com.intellij.ui.util.preferredWidth
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel

class SGSessionPopup(project: Project) : PopupUpdateProcessor(project) {

  private val sessionManager = project.sgSessionManager
  private val sessions = sessionManager?.getSession() ?: emptyList()
  private val popup: JBPopup
  private val fileListPanel = JPanel()

  init {
    val popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(createMainPanel(), null)
    val selectedSessionName = sessionManager?.activeSession?.name ?: ""
    popupBuilder.setCancelKeyEnabled(true)
      .setCancelCallback { cancel() }
      .setShowBorder(WindowRoundedCornersManager.isAvailable() && SystemInfoRt.isMac)
      .setTitle(
        MyBundle.message(
          "popup_title",
          sessions.size,
          selectedSessionName.ifEmpty { MyBundle.message("default_session") })
      )
      .setResizable(true)
      .setMovable(true)
      .addUserData(sessions)

    popup = popupBuilder.createPopup()
  }

  private fun createMainPanel(): JPanel {
    val panel = JPanel()
    panel.preferredSize = Dimension(800, 600)
    panel.layout = BorderLayout()
    panel.add(createSessionList(), BorderLayout.WEST)
    panel.add(fileListPanel, BorderLayout.CENTER)
    return panel
  }

  private fun createSessionList(): JComponent {
    val listModel = DefaultListModel<String>()
    listModel.addAll(sessions.map { it.name.ifEmpty { MyBundle.message("default_session") } })
    listModel.addElement(MyBundle.message("session_create_new"))
    val list = JBList(listModel)
    list.addListSelectionListener { refreshFileList(list.selectedIndex) }
    list.selectedIndex = 0
    refreshFileList(0)
    list.preferredWidth = 200
    return list
  }

  private fun refreshFileList(selectedIndex: Int) {
    fileListPanel.removeAll()
    val session = sessionManager?.getSession()?.get(selectedIndex)
    if (session != null) {
      val listModel = DefaultListModel<String>()
      listModel.addAll(session.files)
      val fileList = JBList(listModel)
      fileListPanel.layout = BorderLayout()
      fileListPanel.add(fileList, BorderLayout.CENTER)
    }
    fileListPanel.revalidate()
    fileListPanel.repaint()
  }

  fun show() {
    popup.showInFocusCenter()
  }

  private fun cancel(): Boolean {
    popup.cancel()
    return true
  }

  override fun updatePopup(intentionAction: Any?) {}
}