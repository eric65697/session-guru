package com.github.eric65697.sessionguru.actions

import com.github.eric65697.sessionguru.sgSessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SGRestoreAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val sessionManager = event.project?.sgSessionManager ?: return
    sessionManager.restoreSessionFiles(sessionManager.activeSession)
  }
}