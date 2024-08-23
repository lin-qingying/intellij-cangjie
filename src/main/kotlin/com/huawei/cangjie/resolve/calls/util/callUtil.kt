package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.BindingContext.CALL
import com.huawei.cangjie.resolve.BindingContext.RESOLVED_CALL
import com.huawei.cangjie.resolve.caches.analyze
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.CallTransformer
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.tower.NewResolvedCallImpl
import com.huawei.cangjie.resolve.calls.tower.psiCangJieCall
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.returnIfNoDescriptorForDeclarationException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

fun Call.getValueArgumentListOrElement(): CjElement =
    if (this is CallTransformer.CallForImplicitInvoke) {
        outerCall.getValueArgumentListOrElement()
    } else {
        valueArgumentList ?: calleeExpression ?: callElement
    }

enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}

inline fun BindingTrace.reportTrailingLambdaErrorOr(
    expression: CjExpression?,
    originalDiagnostic: (CjExpression) -> Diagnostic
) {
    expression?.let { expr ->
        if (expr is CjLambdaExpression && expr.isTrailingLambdaOnNewLIne) {
            report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(expr))
        } else {
            report(originalDiagnostic(expr))
        }
    }
}

private fun expectedType(call: Call, bindingContext: BindingContext): CangJieType {
    return (call.callElement as? CjExpression)?.let {
        bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, it.getQualifiedExpressionForSelectorOrThis()]
    } ?: TypeUtils.NO_EXPECTED_TYPE
}
fun Call.resolveCandidates(
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade,
    expectedType: CangJieType = expectedType(this, bindingContext),
    filterOutWrongReceiver: Boolean = true,
    filterOutByVisibility: Boolean = true
): Collection<ResolvedCall<FunctionDescriptor>> {
    val resolutionScope = callElement.getResolutionScope(bindingContext, resolutionFacade)
    val inDescriptor = resolutionScope.ownerDescriptor

    val dataFlowInfo = bindingContext.getDataFlowInfoBefore(callElement)
    val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace")
    val callResolutionContext = BasicCallResolutionContext.create(
        bindingTrace, resolutionScope, this, expectedType, dataFlowInfo,
        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
        false, resolutionFacade.languageVersionSettings,
        resolutionFacade.dataFlowValueFactory
    ).replaceCollectAllCandidates(true)

    @OptIn(FrontendInternals::class)
    val callResolver = resolutionFacade.frontendService<CallResolver>()

    val results = callResolver.resolveFunctionCall(callResolutionContext)

    var candidates = results.allCandidates!!

    if (callElement is CjConstructorDelegationCall) { // for "this(...)" delegation call exclude caller from candidates
        inDescriptor as ConstructorDescriptor
        candidates = candidates.filter { it.resultingDescriptor.original != inDescriptor.original }
    }

    if (filterOutWrongReceiver) {
        candidates = candidates.filter {
            it.status != ResolutionStatus.RECEIVER_TYPE_ERROR && it.status != ResolutionStatus.RECEIVER_PRESENCE_ERROR
        }
    }

    if (filterOutByVisibility) {
        candidates = candidates.filter {
            DescriptorVisibilityUtils.isVisible(
                it.getDispatchReceiverWithSmartCast(),
                it.resultingDescriptor,
                inDescriptor,
                resolutionFacade.languageVersionSettings
            )
        }
    }

    return candidates
}


fun Call.hasUnresolvedArguments(bindingContext: BindingContext, statementFilter: StatementFilter): Boolean {
    val arguments = valueArguments.map { it.getArgumentExpression() }
    return arguments.any(fun(argument: CjExpression?): Boolean {
        if (argument == null || ArgumentTypeResolver.isFunctionLiteralOrCallableReference(
                argument,
                statementFilter
            )
        ) return false

        when (val resolvedCall = argument.getResolvedCall(bindingContext)) {
            is MutableResolvedCall<*> -> if (!resolvedCall.hasInferredReturnType()) return false
            is NewResolvedCallImpl<*> -> if (resolvedCall.resultingDescriptor.returnType?.isError == true) return false
        }

        val expressionType = bindingContext.getType(argument)
        return expressionType == null || expressionType.isError
    })
}

fun <C : ResolutionContext<C>> Call.hasUnresolvedArguments(context: ResolutionContext<C>): Boolean =
    hasUnresolvedArguments(context.trace.bindingContext, context.statementFilter)

//
//import com.huawei.cangjie.descriptors.CallableDescriptor
//import com.huawei.cangjie.psi.*
//import com.huawei.cangjie.psi.psiUtil.getCalleeExpressionIfAny
//import com.huawei.cangjie.resolve.BindingContext
//import com.huawei.cangjie.resolve.BindingContext.CALL
//import com.huawei.cangjie.resolve.BindingContext.RESOLVED_CALL
//
//import com.huawei.cangjie.resolve.calls.model.ResolvedCall
//
//fun CjElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
//    return this?.getCall(context)?.getResolvedCall(context)
//}
//fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
//    return context[RESOLVED_CALL, this]
//}
fun CjElement.getCall(context: BindingContext): Call? {
    val element = if (this is CjExpression) CjPsiUtil.deparenthesize(this) else this
    if (element == null) return null

    // Do not use Call bound to outer call expression (if any) to prevent stack overflow during analysis
    if (element is CjCallElement && element.calleeExpression == null) return null

    if (element is CjMatchExpression) {
        val subjectVariable = element.subjectVariable
        if (subjectVariable != null) {
            return subjectVariable.getCall(context) ?: context[CALL, element]
        }
    }

    val reference: CjExpression? = when (val parent = element.parent) {
        is CjInstanceExpressionWithLabel -> parent
        is CjUserType -> parent.parent?.parent as? CjConstructorCalleeExpression
        else -> element.getCalleeExpressionIfAny()
    }
    if (reference != null) {
        return context[CALL, reference]
    }
    return context[CALL, element]
}

fun Call.isSafeCall(): Boolean {
    if (this is CallTransformer.CallForImplicitInvoke) {
        //implicit safe 'invoke'
        if (outerCall.isSemanticallyEquivalentToSafeCall) {
            return true
        }
    }
    return isSemanticallyEquivalentToSafeCall
}

fun CjElement?.getCalleeExpressionIfAny(): CjExpression? =
    when (val element = if (this is CjExpression) CjPsiUtil.deparenthesize(this) else this) {
        is CjSimpleNameExpression -> element
        is CjCallElement -> element.calleeExpression
        is CjQualifiedExpression -> element.selectorExpression.getCalleeExpressionIfAny()
        is CjOperationExpression -> element.operationReference
        else -> null
    }

val CjElement.isFakeElement: Boolean
    get() {
        // Don't use getContainingCjFile() because in IDE we can get an element with JavaDummyHolder as containing file
        val file = containingFile
        return file is CjFile && file.doNotAnalyze != null
    }

fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return context[RESOLVED_CALL, this]
}

fun CjElement.safeAnalyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = try {
    analyze(resolutionFacade, bodyResolveMode)
} catch (e: Exception) {
    e.returnIfNoDescriptorForDeclarationException { BindingContext.EMPTY }
}

fun CjElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return this?.getCall(context)?.getResolvedCall(context)
}

fun PsiElement.isCallableReference(): Boolean =
    this is CjNameReferenceExpression && (parent as? CjCallableReferenceExpression)?.callableReference == this

fun PsiElement.asCallableReferenceExpression(): CjCallableReferenceExpression? =
    when {
        isCallableReference() -> parent as CjCallableReferenceExpression
        this is CjCallableReferenceExpression -> this
        else -> null
    }

fun Call.extractCallableReferenceExpression(): CjCallableReferenceExpression? =
    callElement.asCallableReferenceExpression()

fun CangJieCall.extractCallableReferenceExpression(): CjCallableReferenceExpression? =
    psiCangJieCall.psiCall.extractCallableReferenceExpression()

fun CjExpression.createLookupLocation(): CangJieLookupLocation? =
    if (!isFakeElement) CangJieLookupLocation(this) else null

fun Call.createLookupLocation(): CangJieLookupLocation {
    val calleeExpression = calleeExpression
    val element =
        if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
        else callElement
    return CangJieLookupLocation(element)
}
fun Call.getValueArgumentsInParentheses(): List<ValueArgument> = valueArguments.filterArgsInParentheses()
private fun List<ValueArgument?>.filterArgsInParentheses() = filter { it !is CjLambdaArgument } as List<ValueArgument>

val CjLambdaExpression.isTrailingLambdaOnNewLIne
    get(): Boolean {
        (parent as? CjLambdaArgument)?.let { lambdaArgument ->
            var prevSibling = lambdaArgument.prevSibling

            while (prevSibling != null && prevSibling !is CjElement) {
                if (prevSibling is PsiWhiteSpace && prevSibling.textContains('\n'))
                    return true
                prevSibling = prevSibling.prevSibling
            }
        }

        return false
    }
