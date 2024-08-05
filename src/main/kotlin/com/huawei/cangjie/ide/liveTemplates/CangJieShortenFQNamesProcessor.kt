// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.huawei.cangjie.ide.liveTemplates

import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjDotQualifiedExpression
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
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

