/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.ConstructorDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.STATIC_INSTANCE_ACCESS
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.isStaticContext
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

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
