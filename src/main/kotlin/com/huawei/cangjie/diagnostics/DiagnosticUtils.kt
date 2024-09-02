package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.PsiDiagnosticUtils.Companion.offsetToLineAndColumn
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.calls.context.CallPosition
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.inference.isCaptured
import com.huawei.cangjie.resolve.calls.inference.wrapWithCapturingSubstitution
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.util.getEffectiveExpectedType
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructorSubstitution
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.noExpectedType
import com.huawei.cangjie.types.util.isAny
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

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
                throw (e as RuntimeException?)!!
            }
            if (e is Error) {
                throw (e as Error?)!!
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
                        Errors.SETTER_PROJECTED_OUT.on(
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
            Errors.MEMBER_PROJECTED.on(
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
            Errors.TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS.on(
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
        trace.report(Errors.TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN.on(expression, expectedType))
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
