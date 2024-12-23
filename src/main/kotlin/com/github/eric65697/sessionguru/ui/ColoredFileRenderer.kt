package com.github.eric65697.sessionguru.ui

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import java.io.File
import javax.swing.JList


data class ColoredFile(val file: String, val color: Color? = null)

class ColoredFileRenderer : ColoredListCellRenderer<ColoredFile>() {
  override fun customizeCellRenderer(
    list: JList<out ColoredFile>,
    coloredFile: ColoredFile,
    index: Int,
    selected: Boolean,
    hasFocus: Boolean
  ) {
    val fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(coloredFile.file)
    icon = fileType.icon
    val file = File(coloredFile.file)
    val fileAttributes = if (coloredFile.color != null) {
      SimpleTextAttributes(
        null, // coloredFile.backgroundColor,
        coloredFile.color,
        null,
        SimpleTextAttributes.STYLE_PLAIN,
      )
    } else SimpleTextAttributes.REGULAR_ATTRIBUTES
    append(file.name, fileAttributes, true)
    append(" ${coloredFile.file}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}