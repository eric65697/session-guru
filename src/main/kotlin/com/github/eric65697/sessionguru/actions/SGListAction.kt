package com.github.eric65697.sessionguru.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class SGListAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    Messages.showMessageDialog(event.project, "test", "Title", Messages.getInformationIcon())
  }
}