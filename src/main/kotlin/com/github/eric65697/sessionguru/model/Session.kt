package com.github.eric65697.sessionguru.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
  @SerialName("name")
  val name: String,
  @SerialName("files")
  val files: MutableList<String> = mutableListOf(),
  @SerialName("selected")
  var selected: Boolean = false,
  @SerialName("focused_file")
  var focusedFile: String = "",
  @SerialName("timestamp")
  var timestamp: Long = System.currentTimeMillis(),
)