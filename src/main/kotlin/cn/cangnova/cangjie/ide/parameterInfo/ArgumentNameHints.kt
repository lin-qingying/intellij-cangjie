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

package cn.cangnova.cangjie.ide.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import cn.cangnova.cangjie.builtins.extractParameterNameFromFunctionTypeArgument
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.FunctionInvokeDescriptor
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.startOffset
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall
import cn.cangnova.cangjie.resolve.calls.util.getCall
import cn.cangnova.cangjie.resolve.calls.util.getResolvedCall
import cn.cangnova.cangjie.resolve.calls.util.resolveCandidates
import cn.cangnova.cangjie.resolve.isAnnotationConstructor
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode


fun provideArgumentNameHints(element: CjCallElement): List<InlayInfo> {
    if (element.valueArguments.none { it.getArgumentExpression()?.isUnclearExpression() == true }) return emptyList()
    val ctx = element.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
    val call = element.getCall(ctx) ?: return emptyList()
    val resolvedCall = call.getResolvedCall(ctx)
    if (resolvedCall != null) {
        return getArgumentNameHintsForCallCandidate(resolvedCall, call.valueArgumentList)
    }
    val candidates = call.resolveCandidates(ctx, element.getResolutionFacade())
    if (candidates.isEmpty()) return emptyList()
    candidates.singleOrNull()?.let { return getArgumentNameHintsForCallCandidate(it, call.valueArgumentList) }
    return candidates.map { getArgumentNameHintsForCallCandidate(it, call.valueArgumentList) }.reduce { infos1, infos2 ->
        for (index in infos1.indices) {
            if (index >= infos2.size || infos1[index] != infos2[index]) {
                return@reduce infos1.subList(0, index)
            }
        }
        infos1
    }
}

private fun getArgumentNameHintsForCallCandidate(
    resolvedCall: ResolvedCall<out CallableDescriptor>,
    valueArgumentList: CjValueArgumentList?
): List<InlayInfo> {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor.hasSynthesizedParameterNames() && resultingDescriptor !is FunctionInvokeDescriptor) {
        return emptyList()
    }

    if (resultingDescriptor.valueParameters.size == 1
        && resultingDescriptor.name == resultingDescriptor.valueParameters.single().name
    ) {
        // method name equals to single parameter name
        return emptyList()
    }

    return resolvedCall.valueArguments.mapNotNull { (valueParam: ValueParameterDescriptor, resolvedArg) ->
        if (resultingDescriptor.isAnnotationConstructor() && valueParam.name.asString() == "value") {
            return@mapNotNull null
        }

        if (resultingDescriptor is FunctionInvokeDescriptor &&
            valueParam.type.extractParameterNameFromFunctionTypeArgument() == null
        ) {
            return@mapNotNull null
        }

        if (resolvedArg == resolvedCall.valueArgumentsByIndex?.firstOrNull()
            && resultingDescriptor.valueParameters.firstOrNull()?.name == resultingDescriptor.name) {
            // first argument with the same name as method name
            return@mapNotNull null
        }

        resolvedArg.arguments.firstOrNull()?.let { arg ->
            arg.getArgumentExpression()?.let { argExp ->
                if (!arg.isNamed() &&    !valueParam.name.isSpecial && argExp.isUnclearExpression()) {
                    val prefix = if (valueParam.varargElementType != null) "..." else ""
                    val offset = if (arg == valueArgumentList?.arguments?.firstOrNull() && valueParam.varargElementType != null)
                        valueArgumentList.leftParenthesis?.textRange?.endOffset ?: argExp.startOffset
                    else
                        arg.getSpreadElement()?.startOffset ?: argExp.startOffset
                    return@mapNotNull InlayInfo(prefix + valueParam.name.identifier + ":", offset)
                }
            }
        }
        null
    }
}

private fun CjExpression.isUnclearExpression() = when (this) {
    is CjConstantExpression, is CjThisExpression, is CjBinaryExpression, is CjStringTemplateExpression -> true
    is CjPrefixExpression -> baseExpression is CjConstantExpression && (operationToken == CjTokens.PLUS || operationToken == CjTokens.MINUS)
    else -> false
}
