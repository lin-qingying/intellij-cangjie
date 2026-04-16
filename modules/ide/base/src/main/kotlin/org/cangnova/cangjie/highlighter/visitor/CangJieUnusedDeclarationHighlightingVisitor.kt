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

package org.cangnova.cangjie.highlighter.visitor

import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.InjectedFileViewProvider
import org.cangnova.cangjie.analysis.isInjectedFileShouldBeAnalyzed
import org.cangnova.cangjie.highlighter.CangJieUnusedHighlightingProcessor
import org.cangnova.cangjie.psi.CjFile


internal class CangJieUnusedDeclarationHighlightingVisitor : HighlightVisitor {
    override fun suitableForFile(file: PsiFile): Boolean {
        if (file !is CjFile || file.isCompiled) return false

        val highlightingLevelManager = HighlightingLevelManager.getInstance(file.project)
        if (!highlightingLevelManager.shouldInspect(file)) return false

        val viewProvider = file.viewProvider
        val isInjection = InjectedLanguageManager.getInstance(file.project).isInjectedViewProvider(viewProvider)
        if (isInjection && !viewProvider.isInjectedFileShouldBeAnalyzed) {
            // do not highlight unused declarations in injected code
            return false
        }

        val highlightingManager = HighlightingLevelManager.getInstance(file.project)
        return !highlightingManager.runEssentialHighlightingOnly(file)
    }

    override fun visit(element: PsiElement) {}

    override fun analyze(file: PsiFile, updateWholeFile: Boolean, holder: HighlightInfoHolder, action: Runnable): Boolean {
        if (file !is CjFile) return true

        CangJieUnusedHighlightingProcessor(file).collectHighlights(holder)

        return true
    }

    override fun clone(): HighlightVisitor = CangJieUnusedDeclarationHighlightingVisitor()
}
