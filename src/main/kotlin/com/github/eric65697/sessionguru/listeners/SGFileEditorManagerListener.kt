package com.github.eric65697.sessionguru.listeners

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener

class SGFileEditorManagerListener : FileEditorManagerListener {
  override fun selectionChanged(event: FileEditorManagerEvent) {
    super.selectionChanged(event)
    thisLogger().info("Selection changed - ${event.oldFile?.canonicalPath} to ${event.newFile?.canonicalPath}")
    event.manager.project.getService(SGSessionManager::class.java).onFileSelected(event.newFile ?: return)
  }
}