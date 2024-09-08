package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.ide.refactoring.getLastLambdaExpression
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import com.huawei.cangjie.psi.psiUtil.unpackFunctionLiteral
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
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.smartcasts.SmartCastManager
import com.huawei.cangjie.resolve.calls.tower.NewResolvedCallImpl
import com.huawei.cangjie.resolve.calls.tower.psiCangJieCall
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.resolve.scopes.receivers.ClassQualifier
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.resolve.scopes.receivers.TypeAliasQualifier
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.FlexibleType
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.ReceiverType
import com.huawei.cangjie.utils.getImplicitReceiversWithInstance
import com.huawei.cangjie.utils.returnIfNoDescriptorForDeclarationException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

fun CjExpression.getType(context: BindingContext): CangJieType? {
    val type = context.getType(this)
    if (type != null) return type
    val resolvedCall = this.getResolvedCall(context)
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        return resolvedCall.variableCall.resultingDescriptor.type
    }
    return null
}

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
fun CallTypeAndReceiver<*, *>.receiverTypes(
    bindingContext: BindingContext,
    contextElement: PsiElement,
    moduleDescriptor: ModuleDescriptor,
    resolutionFacade: ResolutionFacade,
    stableSmartCastsOnly: Boolean
): List<CangJieType>? {
    return receiverTypesWithIndex(bindingContext, contextElement, moduleDescriptor, resolutionFacade, stableSmartCastsOnly)?.map { it.type }
}
fun CallableDescriptor.receiverType(): CangJieType? = (dispatchReceiverParameter ?: extensionReceiverParameter)?.type
fun CallTypeAndReceiver<*, *>.receiverTypesWithIndex(
    bindingContext: BindingContext,
    contextElement: PsiElement,
    moduleDescriptor: ModuleDescriptor,
    resolutionFacade: ResolutionFacade,
    stableSmartCastsOnly: Boolean,
    withImplicitReceiversWhenExplicitPresent: Boolean = false
): List<ReceiverType>? {
    val languageVersionSettings = resolutionFacade.languageVersionSettings

    val receiverExpression: CjExpression?
    when (this) {
//        is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
//            if (receiver != null) {
//                return when (val lhs =
//                    bindingContext[BindingContext.DOUBLE_COLON_LHS, receiver] ?: return emptyList()) {
//                    is DoubleColonLHS.Type -> listOf(ReceiverType(lhs.type, 0))
//
//                    is DoubleColonLHS.Expression -> {
//                        val receiverValue = ExpressionReceiver.create(receiver, lhs.type, bindingContext)
//                        receiverValueTypes(
//                            receiverValue, lhs.dataFlowInfo, bindingContext,
//                            moduleDescriptor, stableSmartCastsOnly,
//                            resolutionFacade
//                        ).map { ReceiverType(it, 0) }
//                    }
//                }
//            } else {
//                return emptyList()
//            }
//        }

        is CallTypeAndReceiver.DEFAULT -> receiverExpression = null

        is CallTypeAndReceiver.DOT -> receiverExpression = receiver
        is CallTypeAndReceiver.SAFE -> receiverExpression = receiver

        is CallTypeAndReceiver.OPERATOR -> receiverExpression = receiver

//        is CallTypeAndReceiver.SUPER_MEMBERS -> {
//            val qualifier = receiver.superTypeQualifier
//            return if (qualifier != null) {
//                listOfNotNull(bindingContext.getType(receiver)).map { ReceiverType(it, 0) }
//            } else {
//                val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
//                val classDescriptor =
//                    resolutionScope.ownerDescriptor.parentsWithSelf.firstIsInstanceOrNull<ClassDescriptor>()
//                        ?: return emptyList()
//                classDescriptor.typeConstructor.supertypesWithAny().map { ReceiverType(it, 0) }
//            }
//        }

        is CallTypeAndReceiver.IMPORT_DIRECTIVE,
        is CallTypeAndReceiver.PACKAGE_DIRECTIVE,
        is CallTypeAndReceiver.TYPE,
//        is CallTypeAndReceiver.ANNOTATION,
        is CallTypeAndReceiver.UNKNOWN ->
            return null
    }

    val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

    fun extractReceiverTypeFrom(descriptor: ClassDescriptor): CangJieType? = descriptor.classValueType

    fun tryExtractReceiver(context: BindingContext) = context.get(BindingContext.QUALIFIER, receiverExpression)

    fun tryExtractClassDescriptor(context: BindingContext): ClassDescriptor? =
        (tryExtractReceiver(context) as? ClassQualifier)?.descriptor

    fun tryExtractClassDescriptorFromAlias(context: BindingContext): ClassDescriptor? =
        (tryExtractReceiver(context) as? TypeAliasQualifier)?.classDescriptor

    fun extractReceiverTypeFrom(context: BindingContext, receiverExpression: CjExpression): CangJieType? {
        return context.getType(receiverExpression) ?: tryExtractClassDescriptor(context)?.let {
            extractReceiverTypeFrom(
                it
            )
        }
        ?: tryExtractClassDescriptorFromAlias(context)?.let { extractReceiverTypeFrom(it) }
    }

    val expressionReceiver = receiverExpression?.let {
        val receiverType = extractReceiverTypeFrom(bindingContext, receiverExpression) ?: return emptyList()
        ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
    }

    val implicitReceiverValues = resolutionScope.getImplicitReceiversWithInstance(

    ).map { it.value }

    val dataFlowInfo = bindingContext.getDataFlowInfoBefore(contextElement)

    val result = ArrayList<ReceiverType>()

    var receiverIndex = 0

    fun addReceiverType(receiverValue: ReceiverValue, implicit: Boolean) {
        val types = receiverValueTypes(
            receiverValue, dataFlowInfo, bindingContext, moduleDescriptor, stableSmartCastsOnly,
            resolutionFacade
        )

        types.mapTo(result) { type -> ReceiverType(type, receiverIndex, receiverValue.takeIf { implicit }) }

        receiverIndex++
    }
    if (withImplicitReceiversWhenExplicitPresent || expressionReceiver == null) {
        implicitReceiverValues.forEach { addReceiverType(it, true) }
    }
    if (expressionReceiver != null) {
        addReceiverType(expressionReceiver, false)
    }
    return result
}
fun CjCallExpression.singleLambdaArgumentExpression(): CjLambdaExpression? {
    return lambdaArguments.singleOrNull()?.getArgumentExpression()?.unpackFunctionLiteral() ?: getLastLambdaExpression()
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
@OptIn(FrontendInternals::class)
private fun receiverValueTypes(
    receiverValue: ReceiverValue,
    dataFlowInfo: DataFlowInfo,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor,
    stableSmartCastsOnly: Boolean,
    resolutionFacade: ResolutionFacade
): List<CangJieType> {
    val languageVersionSettings = resolutionFacade.languageVersionSettings
    val dataFlowValueFactory = resolutionFacade.dataFlowValueFactory
    val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, bindingContext, moduleDescriptor)
    return if (dataFlowValue.isStable || !stableSmartCastsOnly) { // we don't include smart cast receiver types for "unstable" receiver value to mark members grayed
        smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
            receiverValue,
            bindingContext,
            moduleDescriptor,
            dataFlowInfo,
            languageVersionSettings,
            dataFlowValueFactory
        )
    } else {
        listOf(receiverValue.type)
    }
}
fun SmartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
    receiverToCast: ReceiverValue,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
    dataFlowInfo: DataFlowInfo,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): List<CangJieType> {
    val variants = getSmartCastVariants(
        receiverToCast,
        bindingContext,
        containingDeclarationOrModule,
        dataFlowInfo,
        languageVersionSettings,
        dataFlowValueFactory
    )
    return variants.filter { type ->
        variants.all { another -> another === type || chooseMoreSpecific(type, another).let { it == null || it === type } }
    }
}
private fun chooseMoreSpecific(type1: CangJieType, type2: CangJieType): CangJieType? {
    val type1IsSubtype = CangJieTypeChecker.DEFAULT.isSubtypeOf(type1, type2)
    val type2IsSubtype = CangJieTypeChecker.DEFAULT.isSubtypeOf(type2, type1)

    if (type1IsSubtype && type2IsSubtype) {
        val flexible1 = type1.unwrap() as? FlexibleType
        val flexible2 = type2.unwrap() as? FlexibleType
        return when {
            flexible1 != null && flexible2 == null -> type2
            flexible2 != null && flexible1 == null -> type1
            else -> null //TODO?
        }
    }

    return type1.takeIf { type1IsSubtype }
        ?: type2.takeIf { type2IsSubtype }
}
