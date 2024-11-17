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

package com.linqingying.cangjie.ide.refactoring.move

import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.linqingying.cangjie.psi.psiUtil.getParentOfTypeAndBranch
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo

sealed class CangJieMoveRenameUsage(
    element: PsiElement,
    reference: PsiReference,
    referencedElement: PsiElement
) : MoveRenameUsageInfo(element, reference, referencedElement) {
    abstract val isInternal: Boolean
    abstract fun refresh(refExpr: CjSimpleNameExpression, referencedElement: PsiElement): UsageInfo

    sealed class Deferred(
        element: PsiElement,
        reference: PsiReference,
        referencedElement: PsiElement
    ) : CangJieMoveRenameUsage(element, reference, referencedElement) {
        abstract fun resolve(newElement: PsiElement): UsageInfo?

        class CallableReference(
            element: PsiElement,
            reference: PsiReference,
            referencedElement: PsiElement,
            val originalFile: PsiFile,
            private val addImportToOriginalFile: Boolean,
            override val isInternal: Boolean
        ) : Deferred(element, reference, referencedElement) {
            override fun refresh(refExpr: CjSimpleNameExpression, referencedElement: PsiElement): UsageInfo {
                return CallableReference(
                    refExpr,
                    refExpr.mainReference,
                    referencedElement,
                    originalFile,
                    addImportToOriginalFile,
                    isInternal
                )
            }

            override fun resolve(newElement: PsiElement): UsageInfo? {
                val target = newElement.unwrapped
                val element = element ?: return null
                val reference = reference ?: return null
                val referencedElement = referencedElement ?: return null
                if (target != null) {
                    element.getStrictParentOfType<CjCallableReferenceExpression>()?.receiverExpression?.delete()
                    return Unqualifiable(
                        element,
                        reference,
                        referencedElement,
                        element.containingFile!!,
                        addImportToOriginalFile,
                        isInternal
                    )
                }
                return Qualifiable(element, reference, referencedElement, isInternal)
            }
        }
    }

    class Unqualifiable(
        element: PsiElement,
        reference: PsiReference,
        referencedElement: PsiElement,
        val originalFile: PsiFile,
        val addImportToOriginalFile: Boolean,
        override val isInternal: Boolean
    ) : CangJieMoveRenameUsage(element, reference, referencedElement) {
        override fun refresh(refExpr: CjSimpleNameExpression, referencedElement: PsiElement): UsageInfo {
            return Unqualifiable(
                refExpr,
                refExpr.mainReference,
                referencedElement,
                originalFile,
                addImportToOriginalFile,
                isInternal
            )
        }
    }

    class Qualifiable(
        element: PsiElement,
        reference: PsiReference,
        referencedElement: PsiElement,
        override val isInternal: Boolean
    ) : CangJieMoveRenameUsage(element, reference, referencedElement) {
        override fun refresh(refExpr: CjSimpleNameExpression, referencedElement: PsiElement): UsageInfo {
            return Qualifiable(refExpr, refExpr.mainReference, referencedElement, isInternal)
        }
    }


    companion object {
        fun createIfPossible(
            reference: PsiReference,
            referencedElement: PsiElement,
            addImportToOriginalFile: Boolean,
            isInternal: Boolean
        ): UsageInfo? {
            val element = reference.element

            fun createQualifiable() = Qualifiable(element, reference, referencedElement, isInternal)

            if (element !is CjSimpleNameExpression) return createQualifiable()

            if (element.getStrictParentOfType<CjSuperExpression>() != null) return null
            val containingFile = element.containingFile ?: return null

            fun createUnQualifiable() = Unqualifiable(
                element, reference, referencedElement, containingFile, addImportToOriginalFile, isInternal
            )

            fun createCallableReference() = Deferred.CallableReference(
                element, reference, referencedElement, containingFile, addImportToOriginalFile, isInternal
            )

            if (isExtensionRef(element) && reference.element.getNonStrictParentOfType<CjImportDirective>() == null) return Unqualifiable(
                element, reference, referencedElement, containingFile, addImportToOriginalFile, isInternal
            )

            element.getParentOfTypeAndBranch<CjCallableReferenceExpression> { callableReference }?.let { callable ->
                if (callable.receiverExpression != null) {
                    return if (isQualifiable(callable)) createCallableReference() else null
                }
                val target = referencedElement.unwrapped
                if (target is CjDeclaration && target.parent is CjFile) return createUnQualifiable()

            }
            return createQualifiable()
        }
    }
}
