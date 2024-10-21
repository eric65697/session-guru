package com.github.eric65697.sessionguru.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
  @SerialName("name")
  val name: String,
  @SerialName("files")
  val files: List<String> = listOf(),
  @SerialName("focused_file")
  val focusedFile: String = "",
  @SerialName("timestamp")
  val timestamp: Long = System.currentTimeMillis(),
)