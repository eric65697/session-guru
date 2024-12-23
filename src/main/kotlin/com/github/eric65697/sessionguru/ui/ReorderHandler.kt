package com.github.eric65697.sessionguru.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.ui.components.JBList
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class ReorderHandler(private val listModel: DefaultListModel<*>, private val moveTo: (IntArray, Int) -> Unit) :
  TransferHandler() {
  private val logger = thisLogger()
  private val localObjectFlavor = DataFlavor(Unit::class.java, "Not used")
  private var selectedIndices = intArrayOf()

  override fun canImport(support: TransferSupport): Boolean {
    return support.isDataFlavorSupported(localObjectFlavor)
  }

  override fun createTransferable(c: JComponent): Transferable? {
    val list = c as? JBList<*> ?: return null
    selectedIndices = list.selectedIndices
    return object : Transferable {
      override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(localObjectFlavor)

      override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == localObjectFlavor

      override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return list.selectedValuesList.toTypedArray()
      }
    }
  }

  override fun getSourceActions(c: JComponent?): Int = MOVE

  override fun importData(support: TransferSupport): Boolean {
    if (!canImport(support)) return false
    val location = support.dropLocation as JList.DropLocation
    val dropIndex = location.index
    try {
//      val values = support.transferable.getTransferData(localObjectFlavor) as? Array<*> ?: return false
//      if (values.isEmpty() || selectedIndices.size != values.size) return false
      if (dropIndex < 0 || dropIndex > listModel.size) return false
      logger.info("Move item from [${selectedIndices.joinToString(",")}] to $dropIndex")
      moveTo(selectedIndices, dropIndex)
      return true
    } catch (_: UnsupportedFlavorException) {
      // ignore
    } catch (_: IOException) {
      // ignore
    } finally {
      selectedIndices = intArrayOf()
    }
    return false
  }
}

fun <T> MutableList<T>.reorderIndices(selectedIndices: IntArray, toIndex: Int) {
  val newList = mutableListOf<T>()
  forEachIndexed { i, v ->
    if (i == toIndex) {
      newList.addAll(selectedIndices.map { this[it] })
    }
    if (i !in selectedIndices) newList.add(v)
  }
  if (toIndex >= size) newList.addAll(selectedIndices.map { this[it] })
  clear()
  addAll(newList)
}