package com.github.eric65697.sessionguru

import com.github.eric65697.sessionguru.services.SGSessionManager
import com.intellij.openapi.project.Project

val Project.sgSessionManager: SGSessionManager?
  get() = getService(SGSessionManager::class.java)