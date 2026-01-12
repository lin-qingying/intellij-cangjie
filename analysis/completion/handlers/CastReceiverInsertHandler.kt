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

package org.cangnova.cangjie.completion.handlers

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.completion.DescriptorBasedDeclarationLookupObject
import org.cangnova.cangjie.psi.CjBinaryExpressionWithTypeRHS
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.psi.CjPsiFactory

object CastReceiverInsertHandler {
    fun postHandleInsert(context: InsertionContext, item: LookupElement) {
        val expression =
            PsiTreeUtil.findElementOfClassAtOffset(context.file, context.startOffset, CjSimpleNameExpression::class.java, false)
        val qualifiedExpression = PsiTreeUtil.getParentOfType(expression, CjQualifiedExpression::class.java, true)
        if (qualifiedExpression != null) {
            val receiver = qualifiedExpression.receiverExpression

            val descriptor = (item.`object` as? DescriptorBasedDeclarationLookupObject)?.descriptor as CallableDescriptor
            val project = context.project

            val thisObj = descriptor.dispatchReceiverParameter
            val fqName = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(thisObj!!.type.constructor.declarationDescriptor!!)

            val parentCast = CjPsiFactory(project).createExpression("(expr as $fqName)") as CjParenthesizedExpression
            val cast = parentCast.expression as CjBinaryExpressionWithTypeRHS
            cast.left.replace(receiver)

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitDocument(context.document)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

            val expr = receiver.replace(parentCast) as CjParenthesizedExpression

            ShortenReferences.DEFAULT.process((expr.expression as CjBinaryExpressionWithTypeRHS).right!!)
        }
    }
}
