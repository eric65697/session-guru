package com.github.eric65697.sessionguru.ui

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.ui.WindowRoundedCornersManager
import com.intellij.ui.popup.PopupUpdateProcessor
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel

class SGSessionPopup(project: Project) : PopupUpdateProcessor(project) {

  private val sessionManager = project.sgSessionManager
  private val sessions = sessionManager?.getSession() ?: emptyList()
  private val popup: JBPopup

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
    panel.add(JLabel("Total sessions: ${sessions.size}"), BorderLayout.NORTH)
    return panel
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