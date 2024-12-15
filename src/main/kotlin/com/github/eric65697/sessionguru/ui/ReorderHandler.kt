package com.github.eric65697.sessionguru.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.ui.components.JBList
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class ReorderHandler(private val listModel: DefaultListModel<String>, private val moveTo: (Int, Int) -> Unit) :
  TransferHandler() {
  private val logger = thisLogger()
  override fun canImport(support: TransferSupport): Boolean {
    return support.isDataFlavorSupported(DataFlavor.stringFlavor)
  }

  override fun createTransferable(c: JComponent): Transferable? {
    val list = c as? JBList<*>
    return StringSelection(list?.selectedValue as? String ?: return null)
  }

  override fun getSourceActions(c: JComponent?): Int = MOVE

  override fun importData(support: TransferSupport): Boolean {
    if (!canImport(support)) return false
    try {
      val value = support.transferable.getTransferData(DataFlavor.stringFlavor) as? String ?: return false
      if (value.isEmpty()) return false
      val location = support.dropLocation as JList.DropLocation
      val index = location.index
      if (index < 0 || index > listModel.size) return false
      val fromIndex = listModel.indexOf(value)
      if (fromIndex < 0) return false
      val toIndex = index.coerceAtMost(listModel.size - 1)
      logger.info("Move item from $fromIndex [$value] to $toIndex")
      listModel.remove(fromIndex)
      listModel.add(toIndex, value)
      moveTo(fromIndex, toIndex)
      return true
    } catch (_: UnsupportedFlavorException) {
      // ignore
    } catch (_: IOException) {
      // ignore
    }
    return false
  }
}