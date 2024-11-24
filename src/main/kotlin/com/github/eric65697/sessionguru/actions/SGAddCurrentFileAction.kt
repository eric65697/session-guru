package com.github.eric65697.sessionguru.actions

import com.github.eric65697.sessionguru.notifyInfo
import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager

class SGAddCurrentFileAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val virtualFile = FileDocumentManager.getInstance().getFile(
        FileEditorManager.getInstance(event.project ?: return).selectedTextEditor?.document ?: return
      ) ?: return

    val sessionManager = event.project?.sgSessionManager?: return
    val relativePath =  sessionManager.addFile(sessionManager.activeSession.name, virtualFile)
    val session = sessionManager.activeSession.name.ifEmpty { "default" }
    event.project?.notifyInfo("Added to session($session): $relativePath")
  }
}