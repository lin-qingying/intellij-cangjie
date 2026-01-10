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

package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.psi.CjFile
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeInsight.daemon.impl.BackgroundUpdateHighlightersUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly

/**
 * 高亮处理基类 - 提供语法高亮的基础框架
 * Base Highlighting Pass - Provides the foundation framework for syntax highlighting
 *
 * 这个抽象类为仓颉语言的语法高亮处理提供了基础框架。主要职责包括：
 * This abstract class provides the foundation framework for syntax highlighting in CangJie language. Main responsibilities include:
 *
 * 1. 信息收集 (Information Collection)
 *    - 通过 doCollectInformation 方法收集需要高亮的元素
 *    - Collects elements that need highlighting through doCollectInformation method
 *
 * 2. 高亮应用 (Highlight Application)
 *    - 通过 doApplyInformationToEditor 方法将高亮信息应用到编辑器
 *    - Applies highlighting information to the editor through doApplyInformationToEditor method
 *
 * 3. 后台处理 (Background Processing)
 *    - 支持在后台线程中处理高亮信息
 *    - Supports processing highlighting information in background threads
 *
 * 4. 单元测试支持 (Unit Test Support)
 *    - 提供测试模式下的特殊处理机制
 *    - Provides special handling mechanisms for test mode
 *
 * 该类继承自 TextEditorHighlightingPass，是 IntelliJ Platform 高亮系统的重要组成部分。
 * This class extends TextEditorHighlightingPass and is an essential component of the IntelliJ Platform highlighting system.
 */

abstract class AbstractHighlightingPassBase(
    protected val file: CjFile,
    document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {

    override fun doCollectInformation(progress: ProgressIndicator) {
        val holder = HighlightInfoHolder(file)
        runAnnotatorWithContext(file, holder)
        applyInformationInBackground(holder)
    }

    protected open fun runAnnotatorWithContext(element: PsiElement, holder: HighlightInfoHolder) {
    }

    companion object {
        @Volatile
        private var IGNORE_IN_TESTS: Boolean = false


        @TestOnly
        fun <T> ignoreThesePassesInTests(action: () -> T): T {
            assert(isUnitTestMode)
            IGNORE_IN_TESTS = true
            try {
                return action.invoke()
            } finally {
                IGNORE_IN_TESTS = false
            }
        }
    }

    override fun doApplyInformationToEditor() {
    }

    private fun applyInformationInBackground(holder: HighlightInfoHolder) {
        if (IGNORE_IN_TESTS) {
            assert(isUnitTestMode)
            return
        }
        val result: MutableList<HighlightInfo> = ArrayList(holder.size())
        for (i in 0 until holder.size()) {
            result.add(holder.get(i))
        }
        BackgroundUpdateHighlightersUtil.setHighlightersToEditor(
            myProject,
            file,
            myDocument,
            0,
            file.textLength,
            result,
            id
        )
    }

}
