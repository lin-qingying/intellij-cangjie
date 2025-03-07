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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.builtins.isFunctionTypeOrSubtype
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.ConstructorDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.psi.CjBinaryExpression
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjReferenceExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import cn.cangnova.cangjie.resolve.calls.tasks.isDynamic
import cn.cangnova.cangjie.resolve.calls.util.getResolvedCall
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement

internal class FunctionsHighlightingVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitBinaryExpression(expression: CjBinaryExpression) {
        if (expression.operationReference.identifier != null) {
            expression.getResolvedCall(bindingContext)?.let { resolvedCall ->
                highlightCall(expression.operationReference, resolvedCall)
            }
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: CjCallExpression) {
        val callee = expression.calleeExpression
        val resolvedCall = expression.getResolvedCall(bindingContext)
        if (callee is CjReferenceExpression && callee !is CjCallExpression && resolvedCall != null) {
            highlightCall(callee, resolvedCall)
        }

        super.visitCallExpression(expression)
    }

    private fun highlightCall(callee: PsiElement, resolvedCall: ResolvedCall<out CallableDescriptor>) {
        val calleeDescriptor = resolvedCall.resultingDescriptor

        val extensions = CangJieHighlightingVisitorExtension.EP_NAME.extensionList

        val attributesKey = extensions.firstNotNullOfOrNull { extension ->
            extension.highlightCall(callee, resolvedCall)
        } ?:
        when {
//            calleeDescriptor.fqNameOrNull() == CANGJIE_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME -> CangJieHighlightInfoTypeSemanticNames.KEYWORD
            calleeDescriptor.isDynamic() -> CangJieHighlightInfoTypeSemanticNames.DYNAMIC_FUNCTION_CALL
//            calleeDescriptor is FunctionDescriptor && calleeDescriptor.isSuspend -> CangJieHighlightInfoTypeSemanticNames.SUSPEND_FUNCTION_CALL
            resolvedCall is VariableAsFunctionResolvedCall -> {
                val container = calleeDescriptor.containingDeclaration
                val containedInFunctionClassOrSubclass = container is ClassDescriptor && container.defaultType.isFunctionTypeOrSubtype
                if (containedInFunctionClassOrSubclass)
                    CangJieHighlightInfoTypeSemanticNames.VARIABLE_AS_FUNCTION_CALL
                else
                    CangJieHighlightInfoTypeSemanticNames.VARIABLE_AS_FUNCTION_LIKE_CALL
            }

            calleeDescriptor is ConstructorDescriptor -> CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL
            calleeDescriptor !is FunctionDescriptor -> null
         calleeDescriptor is FunctionDescriptor && calleeDescriptor.isExtend -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_FUNCTION_CALL
            calleeDescriptor.extensionReceiverParameter != null -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_FUNCTION_CALL
            DescriptorUtils.isTopLevelDeclaration(calleeDescriptor) -> CangJieHighlightInfoTypeSemanticNames.PACKAGE_FUNCTION_CALL
            else -> CangJieHighlightInfoTypeSemanticNames.FUNCTION_CALL
        }
        attributesKey?.let { key ->
            highlightName(callee, key)
        }
    }
}
