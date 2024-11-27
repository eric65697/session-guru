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
  private lateinit var sessionTab: JBList<String>

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
    popup.setRequestFocus(true)
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
    val selectedIndex = sessionManager?.getSession()?.indexOfFirst { it == sessionManager.activeSession } ?: 0
    sessionTab = JBList(listModel)
    sessionTab.selectedIndex = selectedIndex
    refreshFileList(selectedIndex, true)
    sessionTab.addListSelectionListener { refreshFileList(sessionTab.selectedIndex) }
    sessionTab.preferredWidth = 200
    sessionTab.isFocusable = true
    return sessionTab
  }

  private fun refreshFileList(selectedIndex: Int, init: Boolean = false) {
    fileListPanel.removeAll()
    val session = sessionManager?.getSession()?.getOrNull(selectedIndex)
    if (session != null) {
      val listModel = DefaultListModel<String>()
      listModel.addAll(session.files)
      val fileList = JBList(listModel)
      fileListPanel.layout = BorderLayout()
      fileListPanel.add(fileList, BorderLayout.CENTER)
      sessionManager?.setCurrentSession(session)
      if (!init) {
        popup.setCaption(
          MyBundle.message(
            "popup_title",
            sessions.size,
            session.name.ifEmpty { MyBundle.message("default_session") })
        )
      }
    }
    if (!init) {
      fileListPanel.revalidate()
      fileListPanel.repaint()
    }
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