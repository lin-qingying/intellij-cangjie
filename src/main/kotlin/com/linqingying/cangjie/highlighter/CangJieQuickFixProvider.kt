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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.ide.inspections.suppress.AnnotationHostKind
import com.linqingying.cangjie.psi.CjElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap



interface CangJieQuickFixProvider {
    companion object {
        fun getInstance(project: Project): CangJieQuickFixProvider = project.service()
    }

    fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * Produces fixes for diagnostics from different factories lazily to avoid creation of redundant quick fixes,
     * e.g. in case only the first suitable fix is required.
     */
    fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>>

    fun createSuppressFix(element: CjElement, suppressionKey: String, hostKind: AnnotationHostKind): SuppressIntentionAction
}

object RegisterQuickFixesLaterIntentionAction : IntentionAction {
    override fun getText(): String = ""

    override fun getFamilyName(): String = ""

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = Unit

    override fun startInWriteAction(): Boolean = false
}
