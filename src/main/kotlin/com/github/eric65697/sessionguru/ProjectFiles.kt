package com.github.eric65697.sessionguru

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

fun Project.relativePath(file: String): String {
  if (file.startsWith("jar:")) return file
  val projectPath = guessProjectDir()?.canonicalPath
  if (projectPath.isNullOrEmpty()) return file
  if (file.startsWith(projectPath)) {
    val relative = file.substring(projectPath.length)
    return if (relative.startsWith("/")) relative.substring(1)
    else relative
  }
  return file
}

fun Project.toVirtualFile(file: String): VirtualFile? {
  val localFileSystem = LocalFileSystem.getInstance()
  return localFileSystem.findFileByNioFile(File(file).toPath())
    ?: localFileSystem.findFileByNioFile(
      File(
        guessProjectDir()?.canonicalPath ?: return null,
        file
      ).toPath()
    )
}