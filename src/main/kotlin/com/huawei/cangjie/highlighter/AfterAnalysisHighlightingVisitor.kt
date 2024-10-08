package com.huawei.cangjie.highlighter

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiElement


/**
 * 代码分析后的高亮逻辑
 */
abstract class AfterAnalysisHighlightingVisitor protected constructor(
    holder: HighlightInfoHolder,
    protected var bindingContext: BindingContext
): AbstractHighlightingVisitor(holder)
{
    protected fun attributeKeyForCallFromExtensions(
        expression: CjSimpleNameExpression,
        resolvedCall: ResolvedCall<out CallableDescriptor>
    ): HighlightInfoType? {
        @Suppress("DEPRECATION")
        return CangJieHighlightingVisitorExtension.EP_NAME.extensionList.firstNotNullOfOrNull { extension ->
            extension.highlightCall(expression, resolvedCall)
        }
    }
    protected fun attributeKeyForDeclarationFromExtensions(element: PsiElement, descriptor: DeclarationDescriptor): HighlightInfoType? {
        @Suppress("DEPRECATION")
        return CangJieHighlightingVisitorExtension.EP_NAME.extensionList.firstNotNullOfOrNull { extension ->
            extension.highlightDeclaration(element, descriptor)
        }
    }
}
