package com.github.eric65697.sessionguru.toolWindow

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.services.MyProjectService
import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import javax.swing.JLabel
import javax.swing.SwingUtilities


class MyToolWindowFactory : ToolWindowFactory {
  init {
    thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val myToolWindow = MyToolWindow(toolWindow)
    val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
    toolWindow.contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project) = true

  class MyToolWindow(toolWindow: ToolWindow) {
    private val logger = thisLogger()
    private val service = toolWindow.project.service<MyProjectService>()
    private val sessionManager = toolWindow.project.service<SGSessionManager>()
    private lateinit var panel: DialogPanel
    private lateinit var sessionPanel: Placeholder
    private lateinit var label: JLabel
    private val alarm = Alarm(sessionManager)

    fun getContent(): DialogPanel {
      logger.debug("getContent: ${sessionManager.getSession().size}")
      panel = panel {
        row {
          label = label(MyBundle.message("randomLabel", "?")).component
          button(
            MyBundle.message("shuffle")
          ) {
            label.text = MyBundle.message("randomLabel", service.getRandomNumber())
          }
        }
        row {
          sessionPanel = placeholder()
        }
      }
      SwingUtilities.invokeLater { initValidation() }
      return panel
    }

    private fun updateSessionList() {
      val sessionList = sessionManager.getSession()
      if (sessionList.isEmpty()) return
      sessionPanel.component?.let {
        logger.debug("updateSessionList: remove session panel")
        panel.remove(it)
      }
      logger.debug("updateSessionList: adding session panel")
      sessionPanel.component = panel {
        sessionList.forEach { session ->
          val name = session.name.ifEmpty { MyBundle.message("default_session") }
          logger.debug("updateSessionList: adding session $name, files: ${session.files.size}")
          collapsibleGroup(name) {
            row {
              cell(JBList<String>().apply {
                setListData(session.files.toTypedArray())
              })
            }
          }.expanded = true
        }
      }
      panel.revalidate()
      panel.repaint()
    }

    private fun initValidation() {
      alarm.addRequest({
        updateSessionList()
        initValidation()
      }, 1000)
    }
  }
}