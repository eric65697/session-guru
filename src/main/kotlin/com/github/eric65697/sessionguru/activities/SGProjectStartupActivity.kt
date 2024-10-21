package com.github.eric65697.sessionguru.activities

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class SGProjectStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val sessionManager = project.getService(SGSessionManager::class.java)
    val fileDocumentManager = FileDocumentManager.getInstance()
    val virtualFiles =
      EditorFactory.getInstance().allEditors.mapNotNull { fileDocumentManager.getFile(it.document) }
    sessionManager.onFileOpened(virtualFiles)
    thisLogger().debug("${project.name} opened with ${virtualFiles.size} editors")
    project.getService(SGSessionManager::class.java).load()
  }
}