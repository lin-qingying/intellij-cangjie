/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.psi.CjFile
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeInsight.daemon.impl.BackgroundUpdateHighlightersUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly


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
            assert(ApplicationManager.getApplication().isUnitTestMode)
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
            assert(ApplicationManager.getApplication().isUnitTestMode)
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
