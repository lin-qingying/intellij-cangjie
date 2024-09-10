package com.huawei.cangjie.ide.highlighter

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement


//对外扩展
abstract class CangJieHighlightingVisitorExtension {
    abstract fun highlightDeclaration(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor): HighlightInfoType?

    open fun highlightCall(elementToHighlight: PsiElement, resolvedCall: ResolvedCall<*>): HighlightInfoType? {
        return highlightDeclaration(elementToHighlight, resolvedCall.resultingDescriptor)
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<CangJieHighlightingVisitorExtension>("com.huawei.cangjie.highlighterExtension")
    }
}
