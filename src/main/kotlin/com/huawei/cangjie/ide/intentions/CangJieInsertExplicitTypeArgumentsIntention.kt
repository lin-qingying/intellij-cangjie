package com.huawei.cangjie.ide.intentions

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.ide.ShortenReferences
import com.huawei.cangjie.ide.quickfix.CangJieSingleIntentionActionFactory
import com.huawei.cangjie.psi.CjCallElement
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjPsiFactory
import com.huawei.cangjie.psi.CjTypeArgumentList
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.calls.inference.CapturedType
import com.huawei.cangjie.resolve.calls.tower.NewResolvedCallImpl
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.types.DefinitelyNotNullType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.NewCapturedType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange


class CangJieInsertExplicitTypeArgumentsIntention : SelfTargetingRangeIntention<CjCallExpression>(
    CjCallExpression::class.java,
    CangJieBundle.lazyMessage("add.explicit.type.arguments")
), LowPriorityAction {
    override fun applicabilityRange(element: CjCallExpression): TextRange? =
        if (isApplicableTo(element)) element.calleeExpression?.textRange else null

    override fun applyTo(element: CjCallExpression, editor: Editor?) = applyTo(element)

    companion object : CangJieSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction = CangJieInsertExplicitTypeArgumentsIntention()

        fun isApplicableTo(element: CjCallElement, bindingContext: BindingContext = element.safeAnalyzeNonSourceRootCode(
            BodyResolveMode.PARTIAL)): Boolean {
            if (element.typeArguments.isNotEmpty()) return false
            if (element.calleeExpression == null) return false

            val resolvedCall = element.getResolvedCall(bindingContext) ?: return false
            val typeArgs = resolvedCall.typeArguments
            val valueParameters = resolvedCall.resultingDescriptor.valueParameters
            if (resolvedCall is NewResolvedCallImpl<*> && valueParameters.any { ErrorUtils.containsErrorType(it.type) }) return false


            return typeArgs.isNotEmpty() && typeArgs.values.none { ErrorUtils.containsErrorType(it) || it is CapturedType || it is NewCapturedType }
        }

        fun applyTo(element: CjCallElement, argumentList: CjTypeArgumentList, shortenReferences: Boolean = true) {
            val callee = element.calleeExpression ?: return
            val newArgumentList = element.addAfter(argumentList, callee) as CjTypeArgumentList
            if (shortenReferences) {
                ShortenReferences.DEFAULT.process(newArgumentList)
            }
        }

        fun applyTo(element: CjCallElement, shortenReferences: Boolean = true) {
            val argumentList = createTypeArguments(element, element.analyze()) ?: return
            applyTo(element, argumentList, shortenReferences)
        }

        fun createTypeArguments(element: CjCallElement, bindingContext: BindingContext): CjTypeArgumentList? {
            val resolvedCall = element.getResolvedCall(bindingContext) ?: return null

            val args = resolvedCall.typeArguments
            val types = resolvedCall.candidateDescriptor.typeParameters

            val text = types.joinToString(", ", "<", ">") {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(args.getValue(it))
            }

            return CjPsiFactory(element.project).createTypeArguments(text)
        }
    }
}
