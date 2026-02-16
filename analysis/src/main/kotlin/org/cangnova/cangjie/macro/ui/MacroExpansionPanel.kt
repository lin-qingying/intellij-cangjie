/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.macro.ui

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.cangnova.cangjie.macro.service.ExpansionSource
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 宏展开结果面板
 *
 * 显示单个宏表达式的展开结果，包含语法高亮。
 */
class MacroExpansionPanel(
    private val project: Project,
    private val result: MacroExpansionResult
) : JPanel(BorderLayout()) {

    private val editor: EditorEx

    init {
        // 创建编辑器显示展开后的代码
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   val document = EditorFactory.getInstance().createDocument(result.expandedText)
        editor = EditorFactory.getInstance().createEditor(document, project) as EditorEx

        // 配置编辑器
        configureEditor(editor)

        // 设置语法高亮
        setupHighlighting(editor)

        // 添加信息标签
        val infoPanel = createInfoPanel()

        // 构建布局
        add(infoPanel, BorderLayout.NORTH)
        add(editor.component, BorderLayout.CENTER)

        // 设置首选大小
        preferredSize = Dimension(600, 400)
        border = JBUI.Borders.empty(5)
    }

    /**
     * 配置编辑器设置
     */
    private fun configureEditor(editor: EditorEx) {
        val settings: EditorSettings = editor.settings
        settings.isLineNumbersShown = true
        settings.isWhitespacesShown = false
        settings.isLineMarkerAreaShown = false
        settings.isIndentGuidesShown = true
        settings.isFoldingOutlineShown = true
        settings.additionalColumnsCount = 3
        settings.additionalLinesCount = 3
        settings.isCaretRowShown = true

        // 设置为只读
        editor.isViewer = true

        // 设置颜色方案
        editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
    }

    /**
     * 设置语法高亮
     */
    private fun setupHighlighting(editor: EditorEx) {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("cj")
        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(
            project,
            fileType
        )
        editor.highlighter = highlighter
    }

    /**
     * 创建信息面板
     */
    private fun createInfoPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)

        val sourceLabel = JBLabel("来源: ${getSourceDisplayName(result.source)}")
        sourceLabel.border = JBUI.Borders.emptyRight(10)

        val locationLabel = if (result.macroName != null) {
            JBLabel("宏: @${result.macroName}")
        } else {
            JBLabel("位置: ${result.startOffset} - ${result.endOffset}")
        }

        val infoBox = JPanel(BorderLayout())
        infoBox.add(sourceLabel, BorderLayout.WEST)
        infoBox.add(locationLabel, BorderLayout.CENTER)

        panel.add(infoBox, BorderLayout.CENTER)

        // 如果有诊断信息，显示警告
        if (result.diagnostics.isNotEmpty()) {
            val warningLabel = JBLabel("⚠ ${result.diagnostics.size} 个诊断信息")
            warningLabel.border = JBUI.Borders.emptyLeft(10)
            panel.add(warningLabel, BorderLayout.EAST)
        }

        return panel
    }

    /**
     * 获取来源的显示名称
     */
    private fun getSourceDisplayName(source: ExpansionSource): String {
        return when (source) {

            ExpansionSource.COMPILER -> "编译器"
            ExpansionSource.CACHE -> "缓存"
        }
    }

    /**
     * 获取首选的可聚焦组件
     */
    fun getPreferredFocusableComponent(): JComponent {
        return editor.contentComponent
    }

    /**
     * 释放资源
     */
    fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}

/**
 * 宏展开结果列表面板
 *
 * 显示文件中所有宏的展开结果。
 */
class MacroExpansionListPanel(
    private val project: Project,
    private val results: List<MacroExpansionResult>
) : JPanel(BorderLayout()) {

    private val editors = mutableListOf<EditorEx>()

    init {
        // 创建主面板
        val contentPanel = JPanel()
        contentPanel.layout = javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS)

        // 为每个结果创建一个展开面板
        for ((index, result) in results.withIndex()) {
            val itemPanel = createItemPanel(index, result)
            contentPanel.add(itemPanel)
            contentPanel.add(javax.swing.Box.createVerticalStrut(10))
        }

        // 添加滚动支持
        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()

        add(scrollPane, BorderLayout.CENTER)

        // 设置首选大小
        preferredSize = Dimension(700, 500)
        border = JBUI.Borders.empty(5)
    }

    /**
     * 为单个结果创建面板
     */
    private fun createItemPanel(index: Int, result: MacroExpansionResult): JPanel {
        val panel = JPanel(BorderLayout())
        val title = if (result.macroName != null) {
            "@${result.macroName}"
        } else {
            "宏 #${index + 1} (${result.startOffset} - ${result.endOffset})"
        }
        panel.border = BorderFactory.createTitledBorder(title)

        // 创建编辑器
        val document = EditorFactory.getInstance().createDocument(result.expandedText)
        val editor = EditorFactory.getInstance().createEditor(document, project) as EditorEx
        editors.add(editor)

        // 配置编辑器
        configureEditor(editor)
        setupHighlighting(editor)

        // 限制高度
        editor.component.preferredSize = Dimension(650, minOf(200, result.expandedText.lines().size * 20 + 40))

        panel.add(editor.component, BorderLayout.CENTER)

        return panel
    }

    /**
     * 配置编辑器
     */
    private fun configureEditor(editor: EditorEx) {
        val settings = editor.settings
        settings.isLineNumbersShown = true
        settings.isWhitespacesShown = false
        settings.isLineMarkerAreaShown = false
        settings.isIndentGuidesShown = true
        settings.isFoldingOutlineShown = false
        settings.additionalColumnsCount = 1
        settings.additionalLinesCount = 1
        settings.isCaretRowShown = false

        editor.isViewer = true
        editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
    }

    /**
     * 设置语法高亮
     */
    private fun setupHighlighting(editor: EditorEx) {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("cj")
        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(
            project,
            fileType
        )
        editor.highlighter = highlighter
    }

    /**
     * 获取首选的可聚焦组件
     */
    fun getPreferredFocusableComponent(): JComponent {
        return this
    }

    /**
     * 释放资源
     */
    fun dispose() {
        for (editor in editors) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
        editors.clear()
    }
}
