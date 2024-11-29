package com.github.eric65697.sessionguru.ui

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.util.preferredWidth
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class SGSessionDialog(private val project: Project) : DialogWrapper(project) {
  private val sessionManager = project.sgSessionManager
  private val sessions = sessionManager?.getSession() ?: emptyList()
  private val fileListPanel = JPanel()
  private lateinit var sessionTab: JBList<String>

  init {
    sessionManager?.activeSession?.let { session ->
      title = MyBundle.message(
        "popup_title",
        sessions.size,
        session.name.ifEmpty { MyBundle.message("default_session") })
    }
    init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel()
    panel.preferredSize = Dimension(800, 600)
    panel.layout = BorderLayout()
    panel.add(createSessionList(), BorderLayout.WEST)
    panel.add(fileListPanel, BorderLayout.CENTER)
    return panel
  }

  private fun createSessionList(): JComponent {
    val listModel = CollectionListModel<String>()
    listModel.add(sessions.map { it.name.ifEmpty { MyBundle.message("default_session") } })
    val selectedIndex = sessionManager?.getSession()?.indexOfFirst { it == sessionManager.activeSession } ?: 0
    sessionTab = JBList(listModel)
    val toolbarDecorator = ToolbarDecorator.createDecorator(sessionTab)
    sessionTab.selectedIndex = selectedIndex
    refreshFileList(selectedIndex, true)
    sessionTab.addListSelectionListener { refreshFileList(sessionTab.selectedIndex) }
    sessionTab.isFocusable = true

    toolbarDecorator.setAddAction { createNewSession() }
    toolbarDecorator.setRemoveAction { removeCurrentSession() }
    val toolbarPanel = toolbarDecorator.createPanel()
    toolbarPanel.preferredWidth = 200
    return toolbarPanel
  }

  private fun refreshSessionTab() {
    if (sessionManager == null) return
    val listModel = CollectionListModel<String>()
    listModel.add(sessions.map { it.name.ifEmpty { MyBundle.message("default_session") } })
    val selectedIndex = sessionManager.getSession().indexOfFirst { it == sessionManager.activeSession }
    sessionTab.model = listModel
    sessionTab.selectedIndex = selectedIndex
    sessionTab.revalidate()
    sessionTab.repaint()
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
        title = MyBundle.message(
          "popup_title",
          sessions.size,
          session.name.ifEmpty { MyBundle.message("default_session") })
      }
    }
    if (!init) {
      fileListPanel.revalidate()
      fileListPanel.repaint()
    }
  }

  private fun createNewSession() {
    val name = Messages.showInputDialog(
      project,
      MyBundle.message("dialog_title_new_session"),
      MyBundle.message("session_name"),
      Messages.getQuestionIcon()
    )
    if (!name.isNullOrEmpty()) {
      if (sessionManager?.createSession(name) != true) return
      refreshSessionTab()
    }
  }

  private fun removeCurrentSession() {
    val session = sessionManager?.activeSession ?: return
    sessionManager.removeSession(session.name)
    refreshSessionTab()
  }


  override fun createActions(): Array<Action> {
    return super.createActions()
  }

  override fun createLeftSideActions(): Array<Action> {
    return arrayOf(addAllOpenFilesAction)
  }


  private val addAllOpenFilesAction: Action
    get() = object : DialogWrapperAction(MyBundle.message("add_open_files")) {
      override fun doAction(event: ActionEvent) {
        val currentSession = sessionManager?.activeSession ?: return
        sessionManager.addAllFiles(currentSession.name,
          FileEditorManager.getInstance(project).allEditors.map { it.file })
        val selectedIndex = sessionManager.getSession().indexOfFirst { it == sessionManager.activeSession }
        refreshFileList(selectedIndex)
      }
    }.apply {
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A)
    }
}