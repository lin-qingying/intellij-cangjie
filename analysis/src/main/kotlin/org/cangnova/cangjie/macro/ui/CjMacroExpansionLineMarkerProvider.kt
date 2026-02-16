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

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.icon.CangJieBaseResourcesIcons
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import org.cangnova.cangjie.macro.service.MacroExpansionService
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.result.CjResult
import java.awt.event.MouseEvent

/**
 * 宏展开行标提供者
 *
 * 在编辑器左侧 gutter 区域为宏表达式显示图标，点击可查看宏展开结果。
 */
internal class CjMacroExpansionLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!Registry.`is`("cangjie.macro.expansion.line.marker.enabled", true)) {
            return null
        }

        // IntelliJ 要求行标锚定在叶子节点上，检查当前元素是否是 CjMacroExpression 的 '@' token
        if (element.node?.elementType != CjTokens.AT) return null

        val macroExpression = element.parent as? CjMacroExpression ?: return null

        // 确保 '@' 是宏表达式的第一个子节点
        if (macroExpression.firstChild != element) return null

        val shortName = macroExpression.shortName?.asString() ?: "macro"
        val tooltipText = "展开宏 @$shortName"

        return LineMarkerInfo(
            element,
            element.textRange,
            CangJieBaseResourcesIcons.MacroCangJie,
            { tooltipText },
            MacroExpansionNavigationHandler(),
            GutterIconRenderer.Alignment.LEFT,
            { tooltipText }
        )
    }

    /**
     * 点击行标图标后的导航处理器
     */
    private class MacroExpansionNavigationHandler : GutterIconNavigationHandler<PsiElement> {
        override fun navigate(e: MouseEvent, element: PsiElement) {
            val macroExpression = element.parent as? CjMacroExpression ?: return
            val project = element.project
            val service = MacroExpansionService.getInstance(project)

            if (!service.isAvailable()) {
                showErrorPopup(project, e, "宏展开服务不可用")
                return
            }

            val file = macroExpression.containingFile.virtualFile ?: return
            val offset = macroExpression.textOffset

            CoroutineScope(Dispatchers.Default).launch {
                val result = service.expandMacroAtOffset(file, offset)

                withContext(Dispatchers.Main) {
                    when (result) {
                        is CjResult.Ok -> showExpansionPopup(project, e, result.ok)
                        is CjResult.Err -> showErrorPopup(project, e, result.err.message)
                    }
                }
            }
        }

        private fun showExpansionPopup(
            project: Project,
            e: MouseEvent,
            result: MacroExpansionResult
        ) {
            val panel = MacroExpansionPanel(project, result)

            val popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, panel.getPreferredFocusableComponent())
                .setTitle("宏展开结果")
                .setResizable(true)
                .setMovable(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .createPopup()

            popup.show(com.intellij.ui.awt.RelativePoint(e))
        }

        private fun showErrorPopup(
            project: Project,
            e: MouseEvent,
            message: String
        ) {
            JBPopupFactory.getInstance()
                .createMessage(message)
                .show(com.intellij.ui.awt.RelativePoint(e))
        }
    }
}
