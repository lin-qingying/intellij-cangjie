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

package org.cangnova.cangjie.analysis

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.InjectedFileViewProvider
import org.jetbrains.annotations.VisibleForTesting

interface CangJieIdeInjectedFilesAnalysisPromoter {
    /**
     * The main entry point to promote highlighting to some visitor.
     */
    fun shouldRunAnalysisForInjectedFile(viewProvider: FileViewProvider): Boolean

    /**
     * This option is used to separate usage of visitors which may produce error highlights from which are not.
     * Should not be used for visitors that do not produce such highlights, as
     * essential highlighting is not meant to generate error diagnostics.
     */
    fun shouldRunOnlyEssentialHighlightingForInjectedFile(psiFile: PsiFile): Boolean

    companion object {
        @VisibleForTesting
        val EP_NAME: ExtensionPointName< CangJieIdeInjectedFilesAnalysisPromoter> =
            ExtensionPointName.Companion.create("org.cangnova.cangjie.cangjieInjectedFilesAnalysisProvider")
    }
}

val FileViewProvider.isInjectedFileShouldBeAnalyzed: Boolean
    get() {
        return this is InjectedFileViewProvider && CangJieIdeInjectedFilesAnalysisPromoter.EP_NAME.extensionList.any { it.shouldRunAnalysisForInjectedFile(this) }
    }

val PsiFile.injectionRequiresOnlyEssentialHighlighting: Boolean
    get() = viewProvider is InjectedFileViewProvider && CangJieIdeInjectedFilesAnalysisPromoter.EP_NAME.extensionList.any { it.shouldRunOnlyEssentialHighlightingForInjectedFile(this) }