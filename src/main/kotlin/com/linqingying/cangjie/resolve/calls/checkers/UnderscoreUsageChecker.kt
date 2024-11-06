package com.linqingying.cangjie.resolve.calls.checkers

import com.intellij.psi.PsiElement
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ConstructorDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.diagnostics.Errors.STATIC_INSTANCE_ACCESS
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.calls.components.isStaticContext
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall

object StaticContextChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        //    调用上下文的Scope是否输入静态声明
        fun isStaticContext(): Boolean {
            return context.scope.isStaticContext()
        }
        if (isStaticContext() && (reportOn is CjThisExpression || reportOn is CjSuperExpression)) {
//           静态上下文调用示例成员
//            val descriptor = resolvedCall.resultingDescriptor

            context.trace.report(

                STATIC_INSTANCE_ACCESS.on(
                    reportOn, reportOn
                )
            )


        }


    }
}

object UnderscoreUsageChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
//        if (resolvedCall is VariableAsFunctionResolvedCall) return
        val descriptor = resolvedCall.resultingDescriptor
        val namedDescriptor: DeclarationDescriptor =
            (descriptor as? ConstructorDescriptor)?.containingDeclaration ?: descriptor
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
