package com.github.eric65697.sessionguru.actions

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.notifyInfo
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager

class SGDeleteCurrentFileAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val virtualFile = FileDocumentManager.getInstance().getFile(
      FileEditorManager.getInstance(event.project ?: return).selectedTextEditor?.document ?: return
    ) ?: return

    val sessionManager = event.project?.sgSessionManager ?: return
    val relativePath = sessionManager.removeFile(sessionManager.activeSession.name, virtualFile)
    val session = sessionManager.activeSession.name.ifEmpty { MyBundle.message("default_session_plain") }
    event.project?.notifyInfo(MyBundle.message("notification_file_removed", session, relativePath))
  }
}