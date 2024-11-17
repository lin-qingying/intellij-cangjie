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

package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjLabelReferenceExpression
import com.linqingying.cangjie.psi.CjReferenceExpression
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.psi.psiUtil.startOffset
import com.linqingying.cangjie.resolve.BindingContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList


class CjSimpleNameReference(expression: CjSimpleNameExpression) : CjSimpleReference<CjSimpleNameExpression>(expression),
    CjReference {
    enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING
    }
    fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement =
        getCjReferenceMutateService().bindToElement(this, element, shorteningMode)
    fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null
    ): PsiElement =
        getCjReferenceMutateService().bindToFqName(this, fqName, shorteningMode, targetElement)

    override val resolvesByNames: Collection<Name>
        get() {

            val element = element




            return listOf(element.getReferencedNameAsName())

        }
    // It's a copy of function in BindingContextUtils supporting some special cases (labels, this)
    private fun CjExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptor = when (this) {
            is CjLabelReferenceExpression -> {
                val target = context[BindingContext.LABEL_TARGET, this]
                target?.let { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] }
            }
            is CjReferenceExpression -> {
                context[BindingContext.REFERENCE_TARGET, this]
            }
            else -> {
                null
            }
        }
        if (descriptor != null) return listOf(descriptor)
        return context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
    }
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return SmartList<DeclarationDescriptor>().apply {
            // Replace Java property with its accessor(s)
            for (descriptor in expression.getReferenceTargets(context)) {
                val sizeBefore = size
//                if (descriptor !is JavaPropertyDescriptor) {
                    add(descriptor)
//                    continue
//                }


//                val readWriteAccess = expression.readWriteAccess(true)
//                descriptor.getter?.let {
//                    if (readWriteAccess.isRead) add(it)
//                }
//                descriptor.setter?.let {
//                    if (readWriteAccess.isWrite) add(it)
//                }

                if (size == sizeBefore) {
                    add(descriptor)
                }
            }
        }
    }


    override fun getRangeInElement(): TextRange {
        val element = element.getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }
}


