package com.github.eric65697.sessionguru.ui

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class SGSessionDialog(private val project: Project) : DialogWrapper(project) {
  private val sessionManager = project.sgSessionManager
  private val sessions = sessionManager.getSession()
  private val fileListPanel = JPanel()
  private lateinit var sessionTab: JBList<String>
  private val sessionTabs: List<String> get() = sessions.map { it.name.ifEmpty { MyBundle.message("default_session") } }

  init {
    sessionManager.activeSession.let { session ->
      title = MyBundle.message(
        "popup_title",
        sessions.size,
        session.name.ifEmpty { MyBundle.message("default_session") })
    }
    init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel()
    panel.preferredSize = Dimension(800, 400)
    panel.layout = BorderLayout()
    panel.add(createSessionList(), BorderLayout.WEST)
    panel.add(fileListPanel, BorderLayout.CENTER)
    return panel
  }

  private fun createSessionList(): JComponent {
    val listModel = DefaultListModel<String>()
    listModel.addAll(sessionTabs)
    val selectedIndex = sessionManager.getSession().indexOfFirst { it == sessionManager.activeSession }
    sessionTab = JBList(listModel)
    val toolbarDecorator = ToolbarDecorator.createDecorator(sessionTab)
    toolbarDecorator.disableUpAction()
    toolbarDecorator.disableDownAction()
    refreshFileList(selectedIndex, true)
    sessionTab.selectedIndex = selectedIndex
    sessionTab.apply {
      addListSelectionListener { refreshFileList(sessionTab.selectedIndex) }
      isFocusable = true
      dragEnabled = true
      dropMode = DropMode.INSERT
      transferHandler = ReorderHandler(listModel) { selectedIndices, toIndex ->
        sessionManager.moveSession(selectedIndices, toIndex)
        listModel.removeAllElements()
        listModel.addAll(sessionTabs)
        sessionTab.selectedIndex = toIndex.coerceAtMost(sessions.lastIndex)
      }
    }

    ListSpeedSearch.installOn(sessionTab)
    toolbarDecorator.setAddAction { createNewSession() }
    toolbarDecorator.setRemoveAction { removeSelectedSession(sessionTab.selectedIndex) }
    val toolbarPanel = toolbarDecorator.createPanel()
    val preferredSize = toolbarPanel.preferredSize
    preferredSize.width = 200
    toolbarPanel.preferredSize = preferredSize
    return toolbarPanel
  }

  private fun refreshSessionTab() {
    val listModel = DefaultListModel<String>()
    listModel.addAll(sessionTabs)
    val selectedIndex = sessionManager.getSession().indexOfFirst { it == sessionManager.activeSession }
    sessionTab.model = listModel
    sessionTab.selectedIndex = selectedIndex
    sessionTab.revalidate()
    sessionTab.repaint()
  }

  private fun refreshFileList(selectedIndex: Int, init: Boolean = false) {
    fileListPanel.removeAll()
    val session = sessionManager.getSession().getOrNull(selectedIndex) ?: return
    val listModel = DefaultListModel<ColoredFile>()
    sessionManager.getFileColorsAsync(session.files) {
      listModel.addAll(it)
    }
    val fileList = JBList(listModel)
    fileList.isFocusable = true
    fileList.cellRenderer = ColoredFileRenderer()
    fileList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val index = fileList.locationToIndex(e.point)
          openFileInList(listModel, index)
        }
      }
    })
    fileList.dragEnabled = true
    fileList.dropMode = DropMode.INSERT
    fileList.transferHandler = ReorderHandler(listModel) { selectedIndices, toIndex ->
      sessionManager.reorderFile(session, selectedIndices, toIndex)
      listModel.removeAllElements()
      sessionManager.getFileColorsAsync(session.files) {
        listModel.addAll(it)
      }
    }

    ListSpeedSearch.installOn(fileList) { coloredFile ->
      val file = File(coloredFile.file)
      file.name.ifEmpty { coloredFile.file }
    }
    val toolbarDecorator = ToolbarDecorator.createDecorator(fileList)
    toolbarDecorator.disableAddAction()
    toolbarDecorator.disableUpAction()
    toolbarDecorator.disableDownAction()
    toolbarDecorator.setRemoveAction { removeFileFromSession(session.name, fileList) }
    fileListPanel.layout = BorderLayout()
    fileListPanel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER)
    sessionManager.setCurrentSession(session)
    if (!init) {
      title = MyBundle.message(
        "popup_title",
        sessions.size,
        session.name.ifEmpty { MyBundle.message("default_session") })
    }
    if (!init) {
      fileListPanel.revalidate()
      fileListPanel.repaint()
    }
    fileList.requestFocus()
  }

  private fun openFileInList(listModel: DefaultListModel<ColoredFile>, index: Int) {
    sessionManager.openFile(listModel.get(index).file)
    doCancelAction()
  }

  private fun removeFileFromSession(name: String, fileList: JBList<ColoredFile>) {
    val selected = fileList.selectedIndices
    val listModel = fileList.model as DefaultListModel<ColoredFile>
    val session = sessionManager.removeFiles(name, selected) ?: return
    listModel.removeAllElements()
    sessionManager.getFileColorsAsync(session.files) {
      listModel.addAll(it)
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
      if (!sessionManager.createSession(name)) return
      refreshSessionTab()
    }
  }

  private fun removeSelectedSession(index: Int) {
    val session = sessionManager.getSession().getOrNull(index) ?: return
    sessionManager.removeSession(session.name)
    refreshSessionTab()
  }

  override fun createActions(): Array<Action> = arrayOf(cancelAction)

  override fun createLeftSideActions(): Array<Action> = arrayOf(addAllOpenFilesAction, restoreSessionAction)

  private val addAllOpenFilesAction: Action
    get() = object : DialogWrapperAction(MyBundle.message("add_open_files")) {
      override fun doAction(event: ActionEvent) {
        val currentSession = sessionManager.activeSession
        sessionManager.addAllFiles(currentSession.name,
          FileEditorManager.getInstance(project).allEditors.map { it.file })
        val selectedIndex = sessionManager.getSession().indexOfFirst { it == sessionManager.activeSession }
        refreshFileList(selectedIndex)
      }
    }.apply {
      putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A)
    }

  private val restoreSessionAction: Action
    get() = object : DialogWrapperAction(MyBundle.message("restore_session")) {
      override fun doAction(event: ActionEvent?) {
        val currentSession = sessionManager.activeSession
        sessionManager.restoreSessionFiles(currentSession)
        doCancelAction()
      }
    }
}