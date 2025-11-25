/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.protodebugger.memory.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import org.cangnova.cangjie.protodebugger.memory.*
import java.awt.Color
import java.awt.Font

/**
 * 内存编辑器
 *
 * 封装 IntelliJ Editor，提供内存特定的编辑功能。
 *
 * 功能：
 * - 十六进制编辑
 * - 高亮显示（修改、断点、PC 等）
 * - 地址导航
 * - 选择管理
 */
class MemoryEditor(

    project: Project,
    document: Document,
    parentDisposable: Disposable
) : Disposable {

    val editor: Editor
    private val highlighters = mutableMapOf<AddressRange, RangeHighlighter>()
    private val listeners = mutableListOf<MemoryEditorListener>()

    init {
        // 创建编辑器
        editor = EditorFactory.getInstance().createEditor(document, project)

        // 配置编辑器设置
        configureEditor()

        // 注册监听器
        setupListeners()

        // 注册清理
        Disposer.register(parentDisposable, this)
    }

    /**
     * 配置编辑器
     */
    private fun configureEditor() {
        val settings = editor.settings

        // 显示设置
        settings.isLineNumbersShown = true
        settings.isWhitespacesShown = false
        settings.isLineMarkerAreaShown = true
        settings.isFoldingOutlineShown = false
        settings.isRightMarginShown = false
        settings.isCaretRowShown = true

        // 编辑设置
        settings.isVirtualSpace = false
        settings.isBlockCursor = true
        settings.isBlinkCaret = true

        // 字体设置（等宽字体）
        editor.colorsScheme.editorFontName = "Monospaced"
        editor.colorsScheme.editorFontSize = 12
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 光标移动监听
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val offset = event.caret?.offset ?: return
                notifyCaretMoved(offset)
            }
        })

        // 选择变化监听
        editor.selectionModel.addSelectionListener(object : SelectionListener {
            override fun selectionChanged(event: SelectionEvent) {
                val start = event.newRange.startOffset
                val end = event.newRange.endOffset
                notifySelectionChanged(start, end)
            }
        })

        // 文档变化监听
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                notifyDocumentChanged(event.offset, event.newLength)
            }
        })
    }

    /**
     * 滚动到偏移
     */
    fun scrollToOffset(offset: Int) {
        editor.caretModel.moveToOffset(offset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    /**
     * 滚动到行
     */
    fun scrollToLine(line: Int) {
        editor.caretModel.moveToLogicalPosition(LogicalPosition(line, 0))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    /**
     * 获取当前光标偏移
     */
    fun getCurrentOffset(): Int = editor.caretModel.offset

    /**
     * 获取当前行号
     */
    fun getCurrentLine(): Int = editor.caretModel.logicalPosition.line




    /**
     * 添加监听器
     */
    fun addListener(listener: MemoryEditorListener) {
        listeners.add(listener)
    }

    /**
     * 移除监听器
     */
    fun removeListener(listener: MemoryEditorListener) {
        listeners.remove(listener)
    }

    /**
     * 通知光标移动
     */
    private fun notifyCaretMoved(offset: Int) {
        listeners.forEach { it.onCaretMoved(offset) }
    }

    /**
     * 通知选择变化
     */
    private fun notifySelectionChanged(start: Int, end: Int) {
        listeners.forEach { it.onSelectionChanged(start, end) }
    }

    /**
     * 通知文档变化
     */
    private fun notifyDocumentChanged(offset: Int, length: Int) {
        listeners.forEach { it.onDocumentChanged(offset, length) }
    }

    override fun dispose() {

        listeners.clear()
        EditorFactory.getInstance().releaseEditor(editor)
    }
}

/**
 * 内存编辑器监听器
 */
interface MemoryEditorListener {
    /**
     * 光标移动
     */
    fun onCaretMoved(offset: Int) {}

    /**
     * 选择变化
     */
    fun onSelectionChanged(start: Int, end: Int) {}

    /**
     * 文档变化
     */
    fun onDocumentChanged(offset: Int, length: Int) {}
}