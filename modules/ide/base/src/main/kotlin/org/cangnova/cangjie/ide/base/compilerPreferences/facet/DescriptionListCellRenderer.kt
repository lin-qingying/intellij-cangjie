package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.ui.ColoredListCellRenderer
import javax.swing.JList

/**
 * 描述型列表渲染器。
 *
 * 对位 Kotlin `DescriptionListCellRenderer` 的文件位置。
 * 当前仓颉 facet UI 尚未使用该渲染器，但声明位应保留在同一包中。
 */
abstract class DescriptionListCellRenderer<T> : ColoredListCellRenderer<T>() {
    final override fun customizeCellRenderer(
        list: JList<out T>,
        value: T?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        customizeDescription(list, value, index, selected, hasFocus)
    }

    protected abstract fun customizeDescription(
        list: JList<out T>,
        value: T?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    )
}
