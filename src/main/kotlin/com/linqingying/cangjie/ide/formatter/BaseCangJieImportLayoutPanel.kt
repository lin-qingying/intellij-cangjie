

package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.CangJieBundle

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.DslComponentProperty
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.table.JBTable
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBInsets
import com.linqingying.cangjie.highlighter.CangJieHighlightingColors
import org.jetbrains.annotations.Nls

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultCellEditor
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

open class BaseCangJieImportLayoutPanel(@Nls title: String) : JPanel(BorderLayout()) {
    val packageTable = CangJiePackageEntryTable()
    val layoutTable = createTableForPackageEntries(packageTable)

    init {
        border = IdeBorderFactory.createTitledBorder(
            title,
            false,
            JBInsets.emptyInsets()
        )
        putClientProperty(DslComponentProperty.VISUAL_PADDINGS, UnscaledGaps.EMPTY)
    }

    protected fun addPackage() {
        var row = layoutTable.selectedRow + 1
        if (row < 0) {
            row = packageTable.entryCount
        }
        val entry = CangJiePackageEntry("", true)
        packageTable.insertEntryAt(entry, row)
        refreshTableModel(row)
    }

    protected fun removePackage() {
        var row = layoutTable.selectedRow
        if (row < 0) return

        val entry = packageTable.getEntryAt(row)
        if (entry.isSpecial) return

        TableUtil.stopEditing(layoutTable)
        packageTable.removeEntryAt(row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsDeleted(row, row)

        if (row >= packageTable.entryCount) {
            row--
        }

        if (row >= 0) {
            layoutTable.setRowSelectionInterval(row, row)
        }
    }

    protected fun movePackageUp() {
        val row = layoutTable.selectedRow
        if (row < 1) return

        TableUtil.stopEditing(layoutTable)
        val entry = packageTable.getEntryAt(row)
        val previousEntry = packageTable.getEntryAt(row - 1)
        packageTable.setEntryAt(entry, row - 1)
        packageTable.setEntryAt(previousEntry, row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsUpdated(row - 1, row)
        layoutTable.setRowSelectionInterval(row - 1, row - 1)
    }

    protected fun movePackageDown() {
        val row = layoutTable.selectedRow
        if (row >= packageTable.entryCount - 1) return

        TableUtil.stopEditing(layoutTable)
        val entry = packageTable.getEntryAt(row)
        val nextEntry = packageTable.getEntryAt(row + 1)
        packageTable.setEntryAt(entry, row + 1)
        packageTable.setEntryAt(nextEntry, row)

        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsUpdated(row, row + 1)
        layoutTable.setRowSelectionInterval(row + 1, row + 1)
    }

    private fun refreshTableModel(row: Int) {
        val model = layoutTable.model as AbstractTableModel
        model.fireTableRowsInserted(row, row)
        layoutTable.setRowSelectionInterval(row, row)
        TableUtil.editCellAt(layoutTable, row, 0)
        val editorComp = layoutTable.editorComponent
        if (editorComp != null) {
            IdeFocusManager.getGlobalInstance()
                .doWhenFocusSettlesDown { IdeFocusManager.getGlobalInstance().requestFocus(editorComp, true) }
        }
    }

    protected fun resizeColumns() {
        val packageRenderer: ColoredTableCellRenderer = object : ColoredTableCellRenderer() {
            override fun customizeCellRenderer(
                table: JTable,
                value: Any?,
                selected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ) {
                val entry = packageTable.getEntryAt(row)
                val attributes = CangJieHighlightingColors.KEYWORD.defaultAttributes
                append(CangJieBundle.message("import.text.import"), SimpleTextAttributes.fromTextAttributes(attributes))
                append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)

                when (entry) {
                    CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY -> append(
                        CangJieBundle.message("import.text.all.other.imports"),
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )

                    CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY -> append(
                        CangJieBundle.message("import.text.all.alias.imports"),
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )

                    else -> append(
                        "${entry.packageName}.*",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                    )
                }
            }
        }

        layoutTable.columnModel.apply {
            getColumn(0).cellRenderer = packageRenderer
            getColumn(1).cellRenderer = BooleanTableCellRenderer()

            fixColumnWidthToHeader(1)
        }
    }

    private fun fixColumnWidthToHeader(columnIndex: Int) {
        with(layoutTable) {
            val column = columnModel.getColumn(columnIndex)
            val width = 15 + tableHeader.getFontMetrics(tableHeader.font).stringWidth(getColumnName(columnIndex))

            column.minWidth = width
            column.maxWidth = width
        }
    }
}

class CangJieStarImportLayoutPanel : BaseCangJieImportLayoutPanel(CangJieBundle.message("title.packages.to.use.import.with")) {
    init {
        val importLayoutPanel = ToolbarDecorator.createDecorator(layoutTable)
            .setAddAction { addPackage() }
            .setRemoveAction { removePackage() }
            .setButtonComparator(
                CangJieBundle.message("start.import.button.text.add"),
                CangJieBundle.message("start.import.button.text.remove")
            ).setPreferredSize(Dimension(-1, 100))
            .createPanel()

        add(importLayoutPanel, BorderLayout.CENTER)
        resizeColumns()
    }
}

class CangJieImportOrderLayoutPanel : BaseCangJieImportLayoutPanel(CangJieBundle.message("title.import.layout")) {
    private val cbImportAliasesSeparately = JBCheckBox(CangJieBundle.message("codestyle.layout.import.aliases.separately"))

    init {
        add(cbImportAliasesSeparately, BorderLayout.NORTH)

        val importLayoutPanel = ToolbarDecorator.createDecorator(layoutTable)
            .addExtraAction(
                object : DumbAwareActionButton(CangJieBundle.message("button.add.package"), IconUtil.addPackageIcon) {
                    override fun actionPerformed(event: AnActionEvent) {
                        addPackage()
                    }

                    override fun getShortcut(): ShortcutSet {
                        return CommonShortcuts.getNewForDialogs()
                    }

                    override fun getActionUpdateThread() = ActionUpdateThread.BGT
                }
            )
            .setRemoveAction { removePackage() }
            .setMoveUpAction { movePackageUp() }
            .setMoveDownAction { movePackageDown() }
            .setRemoveActionUpdater {
                val selectedRow = layoutTable.selectedRow
                val entry = if (selectedRow in 0 until packageTable.entryCount) packageTable.getEntryAt(selectedRow) else null

                entry?.isSpecial == false
            }.setButtonComparator(
                CangJieBundle.message("import.order.button.text.add.package"),
                CangJieBundle.message("import.order.button.text.remove"),
                CangJieBundle.message("import.order.button.text.up"),
                CangJieBundle.message("import.order.button.text.down")
            ).setPreferredSize(Dimension(-1, 100))
            .createPanel()

        add(importLayoutPanel, BorderLayout.CENTER)
        resizeColumns()

        cbImportAliasesSeparately.addItemListener {
            if (areImportAliasesEnabled()) {
                if (CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY !in packageTable.getEntries()) {
                    packageTable.addEntry(CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY)
                    val row = packageTable.entryCount - 1
                    val model = layoutTable.model as AbstractTableModel
                    model.fireTableRowsInserted(row, row)
                    layoutTable.setRowSelectionInterval(row, row)
                }
            } else {
                val entryIndex = packageTable.getEntries().indexOf(CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY)

                if (entryIndex != -1) {
                    val currentIndex = layoutTable.selectedRow
                    packageTable.removeEntryAt(entryIndex)
                    val model = layoutTable.model as AbstractTableModel
                    model.fireTableRowsDeleted(entryIndex, entryIndex)

                    if (currentIndex < entryIndex) {
                        layoutTable.setRowSelectionInterval(currentIndex, currentIndex)
                    } else if (entryIndex > 0) {
                        layoutTable.setRowSelectionInterval(entryIndex - 1, entryIndex - 1)
                    }
                }
            }
        }
    }

    fun recomputeAliasesCheckbox() {
        cbImportAliasesSeparately.isSelected = CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY in packageTable.getEntries()
    }

    private fun areImportAliasesEnabled(): Boolean {
        return cbImportAliasesSeparately.isSelected
    }
}

fun createTableForPackageEntries(packageTable: CangJiePackageEntryTable): JBTable {
    val names = arrayOf(CangJieBundle.message("listbox.import.package"), CangJieBundle.message("listbox.import.with.subpackages"))
    val packageNameColumnIndex = 0
    val withSubpackagesColumnIndex = 1

    val dataModel = object : AbstractTableModel() {
        override fun getColumnCount(): Int {
            return names.size
        }

        override fun getRowCount(): Int {
            return packageTable.entryCount
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val entry = packageTable.getEntryAt(rowIndex)
            if (!isCellEditable(rowIndex, columnIndex)) return null

            return when (columnIndex) {
                packageNameColumnIndex -> entry.packageName
                withSubpackagesColumnIndex -> entry.withSubpackages
                else -> throw IllegalArgumentException(columnIndex.toString())
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            val entry = packageTable.getEntryAt(rowIndex)
            return !entry.isSpecial
        }

        override fun getColumnName(column: Int): String {
            return names[column]
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                packageNameColumnIndex -> String::class.java
                withSubpackagesColumnIndex -> Boolean::class.javaObjectType
                else -> throw IllegalArgumentException(columnIndex.toString())
            }
        }

        override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
            val entry = packageTable.getEntryAt(rowIndex)

            val newEntry = when (columnIndex) {
                packageNameColumnIndex -> CangJiePackageEntry((value as String).trim(), entry.withSubpackages)
                withSubpackagesColumnIndex -> CangJiePackageEntry(entry.packageName, value.toString().toBoolean())
                else -> throw IllegalArgumentException(columnIndex.toString())
            }

            packageTable.setEntryAt(newEntry, rowIndex)
        }
    }

    val result = JBTable(dataModel)
    result.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

    val editor = result.getDefaultEditor(String::class.java)
    if (editor is DefaultCellEditor) editor.clickCountToStart = 1

    return result
}

