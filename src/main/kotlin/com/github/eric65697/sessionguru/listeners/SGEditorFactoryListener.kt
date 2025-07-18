package com.github.eric65697.sessionguru.listeners

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class SGEditorFactoryListener : EditorFactoryListener {
  override fun editorCreated(event: EditorFactoryEvent) {
    super.editorCreated(event)
    val virtualFile = event.editor.virtualFile ?: return
    thisLogger().debug("Created editor for ${virtualFile.canonicalPath}")
    event.editor.project?.getService(SGSessionManager::class.java)
      ?.onFileOpened(listOf(virtualFile))
  }

  override fun editorReleased(event: EditorFactoryEvent) {
    super.editorReleased(event)
    val virtualFile = event.editor.virtualFile ?: return
    thisLogger().debug("Released editor for ${event.editor.virtualFile.canonicalPath}")
    event.editor.project?.getService(SGSessionManager::class.java)
      ?.onFileClosed(virtualFile)
  }
}