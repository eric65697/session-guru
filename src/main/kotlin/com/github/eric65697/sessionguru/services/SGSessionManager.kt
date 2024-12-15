package com.github.eric65697.sessionguru.services

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.model.Session
import com.github.eric65697.sessionguru.relativePath
import com.github.eric65697.sessionguru.toVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.inputStreamIfExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.outputStream

@Service(Service.Level.PROJECT)
class SGSessionManager(
  private val project: Project,
  private val cs: CoroutineScope,
) : Disposable {
  init {
    thisLogger().info(MyBundle.message("project_service", project.name))
  }

  private val logger = thisLogger()
  private var closing = false
  private val sessions = arrayListOf<Session>()
  private val projectPath = project.guessProjectDir()?.canonicalPath ?: ""
  private val storagePath: Path by lazy {
    if (projectPath.isEmpty())
      Path(System.getProperty("user.home"), ".idea", project.name)
    else Path(projectPath, ".idea")
  }

  val activeSession: Session
    get() = sessions.first { it.selected }

  fun onFileOpened(virtualFiles: List<VirtualFile>) {
//    val files = virtualFiles.mapNotNull { it.canonicalPath }
//    if (files.isEmpty()) return
//    val session = sessions.addFiles("", files)
//    logger.debug("${session.files.size} files in the default session")
  }

  //
  fun onFileClosed(file: VirtualFile) {
//    if (closing) return
//    val fileToRemove = file.canonicalPath ?: return
//    val session = sessions.removeFiles("", fileToRemove)
//    logger.debug("${session?.files?.size} files left in the default session")
  }

  //
  fun onFileSelected(file: VirtualFile) {
//    val fileToSelect = file.canonicalPath ?: return
//    sessions.changeFocus("", fileToSelect)
//    logger.debug("Selected $fileToSelect")
  }

  fun addFile(sessionName: String, virtualFile: VirtualFile): String {
    val file = project.relativePath(virtualFile.canonicalPath ?: return "")
    sessions.addFiles(sessionName, listOf(file))
    save()
    return file
  }

  fun openFile(file: String) {
    cs.launch(Dispatchers.EDT) {
      val vf = withContext(Dispatchers.IO) {
        val virtualFile = project.toVirtualFile(file)
        if (virtualFile == null) logger.warn("File not found: $file")
        virtualFile
      }
      if (vf != null) FileEditorManager.getInstance(project).openFile(vf)
    }
  }

  fun addAllFiles(sessionName: String, virtualFileList: List<VirtualFile>) {
    sessions.addFiles(
      sessionName,
      virtualFileList.mapNotNull { project.relativePath(it.canonicalPath ?: return@mapNotNull null) })
    save()
  }

  fun reorderFile(session: Session, fromIndex: Int, toIndex: Int) {
    val file = session.files.removeAt(fromIndex)
    session.files.add(toIndex, file)
    save()
  }

  fun removeFile(sessionName: String, virtualFile: VirtualFile): String {
    val file = project.relativePath(virtualFile.canonicalPath ?: return "")
    val session = sessions.removeFiles(sessionName, file)
    if (session != null) save()
    return file
  }

  fun removeFiles(sessionName: String, indices: IntArray): Session? {
    val session = sessions.firstOrNull { it.name == sessionName } ?: return null
    indices.reversed().forEach { index ->
      if (index >= 0 && index < session.files.size) {
        session.files.removeAt(index)
      }
    }
    save()
    return session
  }

  fun removeSession(sessionName: String) {
    if (sessionName.isEmpty()) return
    val index = sessions.indexOfFirst { it.name == sessionName }
    if (index == -1) return

    val removed = sessions.removeAt(index)
    if (removed.selected && sessions.size > 0) {
      val next = if (index >= sessions.size) index - 1 else index
      sessions[next].selected = true
    }
    save()
  }

  fun getSession(): List<Session> = sessions

  fun setCurrentSession(session: Session) {
    val currentSession = sessions.first { it.selected }
    val targetSession = sessions.firstOrNull { it.name == session.name }
    if (targetSession != null && currentSession.name != targetSession.name) {
      targetSession.selected = true
      currentSession.selected = false
      save()
    }
  }

  fun createSession(name: String): Boolean {
    if (sessions.firstOrNull { it.name == name } != null) return false
    sessions.forEach { if (it.selected) it.selected = false }
    val session = Session(name = name, selected = true)
    sessions.add(session)
    save()
    return true
  }

  fun moveSession(fromIndex: Int, index: Int) {
    val session = sessions.removeAt(fromIndex)
    sessions.add(index, session)
    save()
  }

  private fun ArrayList<Session>.addFiles(name: String, files: List<String>): Session {
    val session = sessions.firstOrNull { it.name == name } ?: Session(name).also { sessions.add(it) }
    files.forEach { file ->
      if (file !in session.files) session.files.add(file)
    }
    return session
  }

  private fun ArrayList<Session>.removeFiles(name: String, file: String): Session? {
    val session = sessions.firstOrNull { it.name == name } ?: return null
    session.files.remove(file)
    return session
  }

  private fun ArrayList<Session>.changeFocus(name: String, focusedFile: String) {
    val session = sessions.firstOrNull { it.name == name } ?: return
    session.focusedFile = focusedFile
  }

  fun saveOnClosing() {
    closing = true
    save()
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun save() {
    cs.launch {
      try {
        withContext(Dispatchers.IO) {
          Path(storagePath.toCanonicalPath(), SESSION_FILE).outputStream().use {
            Json { prettyPrint = true }.encodeToStream(sessions, it)
          }
        }
        logger.info("Saved ${sessions.size} sessions")
      } catch (e: IOException) {
        logger.error("Failed to save session", e)
      } catch (e: SerializationException) {
        logger.error("Object cannot be serialized", e)
      }
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  suspend fun load() {
    cs.launch {
      val sessionList: List<Session> =
        try {
          withContext(Dispatchers.IO) {
            Path(storagePath.toCanonicalPath(), SESSION_FILE).inputStreamIfExists()?.use {
              Json.decodeFromStream(it)
            } ?: emptyList()
          }
        } catch (e: IOException) {
          logger.error("Failed to load session", e)
          emptyList()
        } catch (e: SerializationException) {
          logger.error("Json cannot be deserialized", e)
          emptyList()
        }
      logger.info("Loaded ${sessionList.size} sessions")
      sessions.addAll(sessionList)
      val selected = sessions.firstOrNull { it.selected }
      if (selected == null) {
        val defaultSession = sessionList.firstOrNull { it.name == "" }
        if (defaultSession == null) {
          sessions.add(Session("", selected = true))
        } else {
          defaultSession.selected = true
        }
      }
    }
  }

  fun restoreSessionFiles(session: Session) {
    val fileEditorManager = FileEditorManager.getInstance(project)
    cs.launch(Dispatchers.EDT) {
      withContext(Dispatchers.IO) {
        session.files.mapNotNull {
          val virtualFile = project.toVirtualFile(it)
          if (virtualFile == null) logger.warn("File not found: $it")
          virtualFile
        }
      }.forEach { fileEditorManager.openFile(it) }
    }
  }

  override fun dispose() {
    logger.info("disposed")
  }

  companion object {
    private const val SESSION_FILE = "session_guru.json"
  }
}