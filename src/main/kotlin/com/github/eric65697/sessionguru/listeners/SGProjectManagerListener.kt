package com.github.eric65697.sessionguru.listeners

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class SGProjectManagerListener : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    super.projectClosingBeforeSave(project)
    thisLogger().info("${project.name} closing before save")
    project.getService(SGSessionManager::class.java).saveOnClosing()
  }
}