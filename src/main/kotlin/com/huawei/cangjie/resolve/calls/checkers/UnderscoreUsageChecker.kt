package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.intellij.psi.PsiElement


object UnderscoreUsageChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
//        if (resolvedCall is VariableAsFunctionResolvedCall) return
        val descriptor = resolvedCall.resultingDescriptor
        val namedDescriptor: DeclarationDescriptor = (descriptor as? ConstructorDescriptor)?.containingDeclaration ?: descriptor
        if (!namedDescriptor.name.asString().isUnderscoreOnlyName()) return
        checkCallElement(resolvedCall.call.callElement, context)
    }

    private fun checkCallElement(cjElement: CjElement, context: CallCheckerContext) {
        when (cjElement) {
            is CjSimpleNameExpression ->
                checkSimpleNameUsage(cjElement, context.trace)
            is CjCallExpression ->
                cjElement.calleeExpression?.let { checkCallElement(it, context) }
        }
    }

    private fun checkSimpleNameUsage(cjName: CjSimpleNameExpression, trace: BindingTrace) {
        if (cjName.text.isUnderscoreOnlyName()) {
            TODO()
//            trace.report(Errors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS.on(cjName))
        }
    }

    fun checkSimpleNameUsage(descriptor: DeclarationDescriptor, cjName: CjSimpleNameExpression, trace: BindingTrace) {
        if (descriptor.name.asString().isUnderscoreOnlyName()) {
            checkSimpleNameUsage(cjName, trace)
        }
    }

    private fun String.isUnderscoreOnlyName() =
        isNotEmpty() && all { it == '_' }

}
