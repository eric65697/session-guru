package com.github.eric65697.sessionguru.services

import com.github.eric65697.sessionguru.MyBundle
import com.github.eric65697.sessionguru.model.Session
import com.github.eric65697.sessionguru.relativePath
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
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
    thisLogger().info(MyBundle.message("projectService", project.name))
  }

  private val logger = thisLogger()
  private var closing = false
  private lateinit var currentSession: Session
  private val sessions = arrayListOf<Session>()
  private val projectPath = project.guessProjectDir()?.canonicalPath ?: ""
  private val storagePath: Path by lazy {
    if (projectPath.isEmpty())
      Path(System.getProperty("user.home"), ".idea", project.name)
    else Path(projectPath, ".idea")
  }

  val activeSession: Session
    get() = currentSession

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

  fun removeFile(sessionName: String, virtualFile: VirtualFile): String {
    val file = project.relativePath(virtualFile.canonicalPath ?: return "")
    val session = sessions.removeFiles(sessionName, file)
    if (session != null) save()
    return file
  }

  fun removeSession(sessionName: String) {
    sessions.removeIf { it.name == sessionName }
    save()
  }

  fun getSession(): List<Session> = sessions

  private fun ArrayList<Session>.addFiles(name: String, files: List<String>): Session {
    val index = indexOfFirst { it.name == name }
    if (index != -1) {
      val newFiles = this[index].files.toMutableList()
      files.forEach {
        if (it !in newFiles) newFiles.add(it)
      }
      this[index] = this[index].copy(files = newFiles, timestamp = System.currentTimeMillis())
      return this[index]
    } else {
      add(Session(name, files))
      return this[lastIndex]
    }
  }

  private fun ArrayList<Session>.removeFiles(name: String, file: String): Session? {
    val index = indexOfFirst { it.name == name }
    if (index != -1) {
      val newFiles = this[index].files.filter { it != file }
      this[index] = this[index].copy(files = newFiles, timestamp = System.currentTimeMillis())
      return this[index]
    }
    return null
  }

  private fun ArrayList<Session>.changeFocus(name: String, focusedFile: String) {
    val index = indexOfFirst { it.name == name }
    if (index != -1) {
      this[index] = this[index].copy(focusedFile = focusedFile, timestamp = System.currentTimeMillis())
    }
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
      sessionList.forEach { session ->
        if (session.name == "") currentSession = session
        sessions.add(session)
      }
      if (!::currentSession.isInitialized) {
        currentSession = Session("")
        sessions.add(currentSession)
      }
    }
  }

  override fun dispose() {
    logger.info("disposed")
  }

  companion object {
    private const val SESSION_FILE = "session_guru.json"
  }
}