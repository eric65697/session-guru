package com.github.eric65697.sessionguru.actions

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class SGRestoreAction : AnAction() {
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return
    val sessionManager = project.getService(SGSessionManager::class.java) ?: return
    val fileEditorManager = FileEditorManager.getInstance(event.project ?: return)
    val localFileSystem = LocalFileSystem.getInstance()
    sessionManager.activeSession.files.mapNotNull {
      localFileSystem.findFileByNioFile(File(it).toPath())
        ?: localFileSystem.findFileByNioFile(
          File(
            project.guessProjectDir()?.canonicalPath ?: return@mapNotNull null,
            it
          ).toPath()
        )
    }.forEach { fileEditorManager.openFile(it) }
  }
}