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

package com.linqingying.cangjie.ide.liveTemplates

import com.linqingying.cangjie.psi.CjCallExpression
import com.linqingying.cangjie.psi.CjDotQualifiedExpression
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.intellij.codeInsight.template.impl.TemplateOptionalProcessor
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.template.Template
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.template.impl.TemplateContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.PsiUtilBase
//
//
//class CangJieShortenFQNamesProcessor : TemplateOptionalProcessor {
//    override fun processText(project: Project, template: Template, document: Document, templateRange: RangeMarker, editor: Editor) {
//        if (!template.isToShortenLongNames) return
//
//        PsiDocumentManager.getInstance(project).commitDocument(document)
//
//        val file = PsiUtilBase.getPsiFileInEditor(editor, project) as? CjFile ?: return
//        ShortenReferencesFacility.getInstance().shorten(file, templateRange.textRange)
//
//        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
//    }
//
//    override fun getOptionName(): String {
//        return CodeInsightBundle.message("dialog.edit.template.checkbox.shorten.fq.names")
//    }
//
//    override fun isEnabled(template: Template): Boolean = template.isToShortenLongNames
//    override fun setEnabled(template: Template, value: Boolean) {}
//    override fun isVisible(template: Template, context: TemplateContext) = false
//}
//
//interface ShortenReferencesFacility {
//    fun shorten(file: CjFile, range: TextRange)
//    fun shorten(element: CjElement): PsiElement?
//
//    companion object {
//        fun getInstance(): ShortenReferencesFacility = service()
//    }
//}

//internal class CangJieShortenReferencesFacility : ShortenReferencesFacility{
//    override fun shorten(file: CjFile, range: TextRange) {
//        ShortenReferences.DEFAULT.process(file, range.startOffset, range.endOffset)
//    }
//
//    override fun shorten(element: CjElement): CjElement {
//        return ShortenReferences.DEFAULT.process(element)
//    }
//}

