package com.huawei.cangjie.ide.refactoring.move

import com.huawei.cangjie.ide.highlighter.unwrapped
import com.huawei.cangjie.references.mainReference
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.psi.psiUtil.getParentOfTypeAndBranch
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
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
