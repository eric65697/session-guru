package com.github.eric65697.sessionguru

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

fun Project.relativePath(file: String): String {
  val projectPath =  guessProjectDir()?.canonicalPath
  if (projectPath.isNullOrEmpty()) return file
  if (file.startsWith(projectPath)) {
    val relative = file.substring(projectPath.length)
    return if (relative.startsWith("/")) relative.substring(1)
    else relative
  }
  return file
}