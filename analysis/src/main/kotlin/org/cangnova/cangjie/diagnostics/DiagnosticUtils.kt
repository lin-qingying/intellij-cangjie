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

package org.cangnova.cangjie.diagnostics


import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjNamedFunction

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.diagnostics.PsiDiagnosticUtils.Companion.offsetToLineAndColumn
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.resolve.DescriptorUtils.getContainingClass
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.call.inference.isCaptured
import org.cangnova.cangjie.resolve.call.inference.wrapWithCapturingSubstitution
import org.cangnova.cangjie.resolve.calls.context.CallPosition
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.resolve.calls.util.getEffectiveExpectedType
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructorSubstitution
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.isAny
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.isNothing

inline fun reportOnDeclaration(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (PsiElement) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        trace.report(what(psiElement))
    }
}

object DiagnosticUtils {
    fun getLineAndColumnInPsiFile(
        file: PsiFile,
        range: TextRange
    ): PsiDiagnosticUtils.LineAndColumn {
        val document = file.viewProvider.document
        return offsetToLineAndColumn(document, range.startOffset)
    }

    fun throwIfRunningOnServer(e: Throwable?) {
        // This is needed for the Web Demo server to log the exceptions coming from the analyzer instead of showing them in the editor.
        if (System.getProperty(
                "cangjie.running.in.server.mode",
                "false"
            ) == "true" || ApplicationManager.getApplication().isUnitTestMode
        ) {
            if (e is RuntimeException) {
                throw (e as RuntimeException?) ?: return
            }
            if (e is Error) {
                throw (e as Error?) ?: return
            }
            throw RuntimeException(e)
        }
    }

}

inline fun reportOnDeclarationOrFail(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (PsiElement) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        trace.report(what(psiElement))
    } ?: throw AssertionError("No declaration for $descriptor")
}

fun BindingTrace.reportDiagnosticOnce(diagnostic: Diagnostic) {
    if (bindingContext.diagnostics.noSuppression().forElement(diagnostic.psiElement)
            .any { it.factory == diagnostic.factory }
    ) return

    report(diagnostic)
}

fun ResolutionContext<*>.reportTypeMismatchDueToTypeProjection(
    expression: CjElement,
    expectedType: CangJieType,
    expressionType: CangJieType?
): Boolean {
    if (!TypeUtils.contains(expectedType) {
            // We have to check expected type is available otherwise we'll get an exception
            !noExpectedType(it) && (it.isAny() || it.isNothing())
        }
    ) return false

    val (resolvedCall, correspondingNotApproximatedTypeByDescriptor: (CallableDescriptor) -> CangJieType?) = when (callPosition) {
        is CallPosition.ValueArgumentPosition ->
            callPosition.resolvedCall to { f: CallableDescriptor ->
                getEffectiveExpectedType(
                    f.valueParameters[callPosition.valueParameter.index],
                    callPosition.valueArgument,
                    this
                )
            }

        is CallPosition.ExtensionReceiverPosition ->
            callPosition.resolvedCall to { f: CallableDescriptor -> f.extensionReceiverParameter?.type }

        is CallPosition.VariableAssignment -> {
            if (callPosition.isLeft) return false
            val resolvedCall = callPosition.leftPart.getResolvedCall(trace.bindingContext) ?: return false
            resolvedCall to { f: CallableDescriptor -> null }
        }
//        is CallPosition.PropertyAssignment -> {
//            if (callPosition.isLeft) return false
//            val resolvedCall = callPosition.leftPart.getResolvedCall(trace.bindingContext) ?: return false
//            resolvedCall to { f: CallableDescriptor -> (f as? PropertyDescriptor)?.setter?.valueParameters?.get(0)?.type }
//        }

        is CallPosition.Unknown/*, is CallPosition.CallableReferenceRhs*/ -> return false

    }

    val receiverType = resolvedCall.smartCastDispatchReceiverType
        ?: (resolvedCall.dispatchReceiver ?: return false).type

    val callableDescriptor = resolvedCall.resultingDescriptor.original

    val substitutedDescriptor =
        TypeConstructorSubstitution
            .create(receiverType)
            .wrapWithCapturingSubstitution(needApproximation = false)
            .buildSubstitutor().let { callableDescriptor.substitute(it) } ?: return false

    val nonApproximatedExpectedType =
        correspondingNotApproximatedTypeByDescriptor(substitutedDescriptor) ?: return false
    if (!TypeUtils.contains(nonApproximatedExpectedType) { it.isCaptured() }) return false

    if (expectedType.isNothing()) {
        /*        if (callPosition is CallPosition.PropertyAssignment) {
                    trace.report(
                         SETTER_PROJECTED_OUT.on(
                            callPosition.leftPart ?: return false,
                            resolvedCall.resultingDescriptor
                        )
                    )
                } else {*/
        val call = resolvedCall.call
        val reportOn =
            if (resolvedCall is VariableAsFunctionResolvedCall)
                resolvedCall.variableCall.call.calleeExpression
            else
                call.calleeExpression

        trace.reportDiagnosticOnce(
            MEMBER_PROJECTED.on(
                reportOn ?: call.callElement,
                callableDescriptor,
                receiverType
            )
        )
//        }
    } else {
        // expressionType can be null when reporting CONSTANT_EXPECTED_TYPE_MISMATCH (see addAll.kt test)
        expressionType ?: return false
        trace.report(
            TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS.on(
                expression, TypeMismatchDueToTypeProjectionsData(
                    expectedType, expressionType, receiverType, callableDescriptor
                )
            )
        )

    }

    return true
}

fun ResolutionContext<*>.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(
    expression: CjElement,
    expectedType: CangJieType,
    expressionType: CangJieType?
): Boolean {
    if (expressionType == null) return false

    if (expressionType.isFunctionType && !expectedType.isFunctionType && isScalaLikeEqualsBlock(expression)) {
        trace.report(TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN.on(expression, expectedType))
        return true
    }

    return false
}

private fun isScalaLikeEqualsBlock(expression: CjElement): Boolean =
    expression is CjLambdaExpression &&
            expression.parent.let { it is CjNamedFunction && it.equalsToken != null }

class TypeMismatchDueToTypeProjectionsData(
    val expectedType: CangJieType,
    val expressionType: CangJieType,
    val receiverType: CangJieType,
    val callableDescriptor: CallableDescriptor
)


class InvalidBinaryData(
    val operatorString: String,
    val leftType: CangJieType,
    val rightType: CangJieType
)
fun MemberDescriptor.isEffectivelyExternal(): Boolean {
//    if (isExternal) return true
//
//    if (this is PropertyAccessorDescriptor) {
//        val variableDescriptor = correspondingProperty
//        if (variableDescriptor.isEffectivelyExternal()) return true
//    }

//    if (this is PropertyDescriptor) {
//        if (getter?.isExternal == true &&
//            (!isVar || setter?.isExternal == true)
//        ) return true
//    }

    val containingClass = getContainingClass(this)
    return containingClass != null && containingClass.isEffectivelyExternal()
}
