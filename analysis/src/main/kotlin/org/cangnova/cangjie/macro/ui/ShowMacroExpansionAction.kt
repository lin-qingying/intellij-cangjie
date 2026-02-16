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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import org.cangnova.cangjie.macro.service.MacroExpansionService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.result.CjResult
import java.awt.Point
import javax.swing.SwingUtilities

/**
 * 显示宏展开结果的动作
 *
 * 当用户将光标放在宏表达式上时，可以通过此动作查看宏展开后的代码。
 *
 * ## 使用方式
 *
 * - 快捷键: `Ctrl+Alt+M`
 * - 右键菜单: "Show Macro Expansion"
 * - 工具菜单: "Tools > CangJie > Show Macro Expansion"
 */
class
ShowMacroExpansionAction : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        // 检查是否是仓颉文件
        val isCjFile = file is CjFile

        // 检查是否有活动的编辑器
        val hasEditor = editor != null && project != null

        // 检查服务是否可用
        val serviceAvailable = project?.let {
            MacroExpansionService.getInstance(it).isAvailable()
        } ?: false

        e.presentation.isEnabled = isCjFile && hasEditor && serviceAvailable
        e.presentation.isVisible = isCjFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        if (psiFile !is CjFile) return

        val offset = editor.caretModel.offset

        // 查找光标位置的宏表达式
        val macroExpression = findMacroExpressionAtOffset(psiFile, offset)

        if (macroExpression != null) {
            // 展开单个宏
            expandAndShowMacro(project, editor, macroExpression)
        } else {
            // 如果光标不在宏上，尝试展开文件中的所有宏
            expandAndShowAllMacros(project, editor, psiFile)
        }
    }

    /**
     * 查找指定偏移量位置的宏表达式
     */
    private fun findMacroExpressionAtOffset(psiFile: CjFile, offset: Int): CjMacroExpression? {
        val element = psiFile.findElementAt(offset) ?: return null
        return PsiTreeUtil.getParentOfType(element, CjMacroExpression::class.java)
    }

    /**
     * 展开并显示单个宏
     */
    private fun expandAndShowMacro(
        project: Project,
        editor: Editor,
        macroExpression: CjMacroExpression
    ) {
        val service = MacroExpansionService.getInstance(project)

        // 在 EDT 上提取 PSI 数据，避免在后台线程访问 PSI
        val file = macroExpression.containingFile?.virtualFile ?: return
        val offset = macroExpression.textOffset

        // 在协程中执行展开操作
        CoroutineScope(Dispatchers.Default).launch {
            val result = service.expandMacroAtOffset(file, offset)

            withContext(Dispatchers.Main) {
                when (result) {
                    is CjResult.Ok -> {
                        showExpansionPopup(project, editor, result.ok)
                    }
                    is CjResult.Err -> {
                        showErrorPopup(project, editor, result.err.message ?: CangJieMacroBundle.message("macro.expansion.failed"))
                    }
                }
            }
        }
    }

    /**
     * 展开并显示文件中的所有宏
     */
    private fun expandAndShowAllMacros(
        project: Project,
        editor: Editor,
        psiFile: CjFile
    ) {
        val virtualFile = psiFile.virtualFile ?: return
        val service = MacroExpansionService.getInstance(project)

        CoroutineScope(Dispatchers.Default).launch {
            val result = service.expandAllMacrosInFile(virtualFile)

            withContext(Dispatchers.Main) {
                when (result) {
                    is CjResult.Ok -> {
                        if (result.ok.isEmpty()) {
                            showInfoPopup(project, editor, CangJieMacroBundle.message("macro.expansion.no.macros.in.file"))
                        } else {
                            showAllExpansionsPopup(project, editor, result.ok)
                        }
                    }
                    is CjResult.Err -> {
                        showErrorPopup(project, editor, result.err.message ?: CangJieMacroBundle.message("macro.expansion.failed"))
                    }
                }
            }
        }
    }

    /**
     * 显示宏展开结果弹窗
     */
    private fun showExpansionPopup(
        project: Project,
        editor: Editor,
        result: MacroExpansionResult
    ) {
        val panel = MacroExpansionPanel(project, result)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel.getPreferredFocusableComponent())
            .setTitle(CangJieMacroBundle.message("macro.expansion.popup.title"))
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setRequestFocus(true)
            .createPopup()

        // 在编辑器光标位置显示弹窗
        val point = editor.visualPositionToXY(editor.caretModel.visualPosition)
        val locationOnScreen = editor.contentComponent.locationOnScreen
        popup.show(RelativePoint(Point(locationOnScreen.x + point.x, locationOnScreen.y + point.y + editor.lineHeight)))
    }

    /**
     * 显示所有宏展开结果弹窗
     */
    private fun showAllExpansionsPopup(
        project: Project,
        editor: Editor,
        results: List<MacroExpansionResult>
    ) {
        val panel = MacroExpansionListPanel(project, results)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel.getPreferredFocusableComponent())
            .setTitle(CangJieMacroBundle.message("macro.expansion.all.popup.title", results.size))
            .setResizable(true)
            .setMovable(true)
            .setFocusable(true)
            .setRequestFocus(true)
            .createPopup()

        popup.showCenteredInCurrentWindow(project)
    }

    /**
     * 显示错误弹窗
     */
    private fun showErrorPopup(
        project: Project,
        editor: Editor,
        message: String
    ) {
        JBPopupFactory.getInstance()
            .createMessage(message)
            .showInBestPositionFor(editor)
    }

    /**
     * 显示信息弹窗
     */
    private fun showInfoPopup(
        project: Project,
        editor: Editor,
        message: String
    ) {
        JBPopupFactory.getInstance()
            .createMessage(message)
            .showInBestPositionFor(editor)
    }
}
