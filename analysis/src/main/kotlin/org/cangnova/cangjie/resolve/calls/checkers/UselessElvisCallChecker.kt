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

import org.cangnova.cangjie.diagnostics.reportDiagnosticOnce
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils
import org.cangnova.cangjie.types.isError
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.infos.warnings.USELESS_ELVIS
import org.cangnova.cangjie.resolve.calls.smartcasts.OptionStatus
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.contains

class UselessElvisCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor.name != ControlStructureTypingUtils.ResolveConstruct.ELVIS.specialFunctionName) return

        val elvisBinaryExpression = resolvedCall.call.callElement as? CjBinaryExpression ?: return
        val left = elvisBinaryExpression.left ?: return
        val right = elvisBinaryExpression.right ?: return

        val leftType = context.trace.getType(left) ?: return

        // if type contains not fixed `TypeVariable` it means that call wasn't completed, we should wait for its completion first
        if (leftType.isError || leftType.contains { it.constructor is TypeVariableTypeConstructor }) return

        if (!TypeUtils.isOptionType(leftType)) {
            context.trace.reportDiagnosticOnce(USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

        val dataFlowValue = context.dataFlowValueFactory.createDataFlowValue(left, leftType, context.resolutionContext)
        if (context.dataFlowInfo.getStableOptionStatus(dataFlowValue) == OptionStatus.DEFINITE) {
            context.trace.reportDiagnosticOnce(USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

//        if (CjPsiUtil.isOptionConstant(right) && !leftType.isNullabilityFlexible()) {
//            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS_RIGHT_IS_NULL.on(elvisBinaryExpression))
//        }
    }
}
