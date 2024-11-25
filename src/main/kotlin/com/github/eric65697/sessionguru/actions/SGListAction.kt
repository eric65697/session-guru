package com.github.eric65697.sessionguru.actions

import com.github.eric65697.sessionguru.ui.SGSessionPopup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SGListAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    SGSessionPopup(event.project ?: return).show()
  }
}