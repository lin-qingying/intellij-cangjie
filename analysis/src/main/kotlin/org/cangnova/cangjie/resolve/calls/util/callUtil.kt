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

package org.cangnova.cangjie.resolve.calls.util


import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.firstIsInstanceOrNull
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.cangnova.cangjie.psi.psiUtil.unpackFunctionLiteral
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.RESOLVED_CALL
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoBefore
import org.cangnova.cangjie.resolve.caches.analyze
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.components.isVararg
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.smartcasts.SmartCastManager
import org.cangnova.cangjie.resolve.calls.tower.NewResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.tower.psiCangJieCall
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.ClassQualifier
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.resolve.scopes.receivers.TypeAliasQualifier
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.FlexibleType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.classValueType
import org.cangnova.cangjie.utils.getImplicitReceiversWithInstance
import org.cangnova.cangjie.utils.getLastLambdaExpression
import org.cangnova.cangjie.utils.returnIfNoDescriptorForDeclarationException
import org.cangnova.cangjie.utils.supertypesWithAny

fun CjElement?.getParentResolvedCall(
    context: BindingContext,
    strict: Boolean = true
): ResolvedCall<out CallableDescriptor>? {
    return this?.getParentCall(context, strict)?.getResolvedCall(context)
}


fun <D : CallableDescriptor> ResolvedCall<D>.hasTypeMismatchErrorOnParameter(parameter: ValueParameterDescriptor): Boolean {
    val resolvedValueArgument = valueArguments[parameter]
    if (resolvedValueArgument == null) return true

    return resolvedValueArgument.arguments.any { argument ->
        val argumentMapping = getArgumentMapping(argument)
        argumentMapping is ArgumentMatch && argumentMapping.status == ArgumentMatchStatus.TYPE_MISMATCH
    }
}

fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedArguments(): Boolean {
    return call.valueArguments.any { argument -> getArgumentMapping(argument) == ArgumentUnmapped }
}

fun CjElement.getParentCall(context: BindingContext, strict: Boolean = true): Call? {
    val callExpressionTypes = arrayOf(
        CjSimpleNameExpression::class.java, CjCallElement::class.java, CjBinaryExpression::class.java,
        CjUnaryExpression::class.java, CjArrayAccessExpression::class.java
    )

    val parent = if (strict) {
        PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
    } else {
        PsiTreeUtil.getNonStrictParentOfType(this, *callExpressionTypes)
    }
    return parent?.getCall(context)
}

fun CjExpression.getType(context: BindingContext): CangJieType? {
    val type = context.getType(this)
    if (type != null) return type
    val resolvedCall = this.getResolvedCall(context)
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        return resolvedCall.variableCall.resultingDescriptor.type
    }
    return null
}

fun <D : CallableDescriptor> ResolvedCall<D>.getParameterForArgument(valueArgument: ValueArgument?): ValueParameterDescriptor? {
    return (valueArgument?.let { getArgumentMapping(it) } as? ArgumentMatch)?.valueParameter
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
            report(UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(expr))
        } else {
            report(originalDiagnostic(expr))
        }
    }
}

fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMapped() =
    call.valueArguments.all { argument -> getArgumentMapping(argument) is ArgumentMatch }

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
): Collection<ResolvedCall<out FunctionDescriptor>> {
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
//import org.cangnova.cangjie.descriptors.CallableDescriptor
//import org.cangnova.cangjie.psi.*
//import org.cangnova.cangjie.psi.psiUtil.getCalleeExpressionIfAny
//import org.cangnova.cangjie.resolveName.BindingContext
//import org.cangnova.cangjie.resolveName.BindingContext.CALL
//import org.cangnova.cangjie.resolveName.BindingContext.RESOLVED_CALL
//
//import org.cangnova.cangjie.resolveName.calls.model.ResolvedCall
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

//    if (element is CjMatchExpression) {
//        val subjectVariable = element.subjectVariable
//        if (subjectVariable != null) {
//            return subjectVariable.getCall(context) ?: context[CALL, element]
//        }
//    }

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

fun Call.getResolvedCall(context: BindingContext): ResolvedCall< CallableDescriptor>? {
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
    return receiverTypesWithIndex(
        bindingContext,
        contextElement,
        moduleDescriptor,
        resolutionFacade,
        stableSmartCastsOnly
    )?.map { it.type }
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
        is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
            return emptyList()

        }

        is CallTypeAndReceiver.DEFAULT -> receiverExpression = null

        is CallTypeAndReceiver.DOT -> receiverExpression = receiver
        is CallTypeAndReceiver.SAFE -> receiverExpression = receiver

        is CallTypeAndReceiver.OPERATOR -> receiverExpression = receiver
        is CallTypeAndReceiver.DELEGATE -> receiverExpression = receiver

        is CallTypeAndReceiver.SUPER_MEMBERS -> {
            val qualifier = receiver.superTypeQualifier
            return if (qualifier != null) {
                listOfNotNull(bindingContext.getType(receiver)).map { ReceiverType(it, 0) }
            } else {
                val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
                val classDescriptor =
                    resolutionScope.ownerDescriptor.parentsWithSelf.firstIsInstanceOrNull<ClassDescriptor>()
                        ?: return emptyList()
                classDescriptor.typeConstructor.supertypesWithAny().map { ReceiverType(it, 0) }
            }
        }

        is CallTypeAndReceiver.IMPORT_DIRECTIVE,
        is CallTypeAndReceiver.PACKAGE_DIRECTIVE,
        is CallTypeAndReceiver.TYPE,
        is CallTypeAndReceiver.ANNOTATION,
        is CallTypeAndReceiver.UNKNOWN ->
            return null
    }

    val resolutionScope = contextElement.getResolutionScope(bindingContext, resolutionFacade)

    fun extractReceiverTypeFrom(descriptor: ClassDescriptor): CangJieType? = descriptor.classValueType

    fun tryExtractReceiver(context: BindingContext) =receiverExpression?.let { context.get(BindingContext.QUALIFIER, receiverExpression) }

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
    this is CjNameReferenceExpression && (parent as? CjCallableReference)?.callableReference == this

fun PsiElement.asCallableReferenceExpression(): CjCallableReference? =
    when {
        isCallableReference() -> parent as CjCallableReference
        this is CjCallableReference -> this
        else -> null
    }

fun Call.extractCallableReferenceExpression(): CjCallableReference? =
    callElement.asCallableReferenceExpression()

fun CangJieCall.extractCallableReferenceExpression(): CjCallableReference? =
    psiCangJieCall.psiCall.extractCallableReferenceExpression()

//fun CangJieCall.extractCallableReferenceExpression(): CjCallableReferenceExpression? =
//    psiCangJieCall.psiCall.extractCallableReferenceExpression()

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
        variants.all { another ->
            another === type || chooseMoreSpecific(
                type,
                another
            ).let { it == null || it === type }
        }
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

/**
 * See `ArgumentsToParametersMapper` class in the compiler.
 */
fun Call.mapArgumentsToParameters(targetDescriptor: CallableDescriptor): Map<ValueArgument, ValueParameterDescriptor> {
    val parameters = targetDescriptor.valueParameters
    if (parameters.isEmpty()) return emptyMap()

    val map = HashMap<ValueArgument, ValueParameterDescriptor>()
    val parametersByName =
        if (targetDescriptor.hasStableParameterNames()) parameters.associateBy { it.name } else emptyMap()

    var positionalArgumentIndex: Int? = 0

    for (argument in valueArguments) {
        if (argument is LambdaArgument) {
            map[argument] = parameters.last()
        } else {
            val argumentName = argument.getArgumentName()?.asName

            if (argumentName != null) {
                val parameter = parametersByName[argumentName]
                if (parameter != null) {
                    map[argument] = parameter
                    if (parameter.index == positionalArgumentIndex) {
                        positionalArgumentIndex++
                        continue
                    }
                }
                positionalArgumentIndex = null
            } else {
                if (positionalArgumentIndex != null && positionalArgumentIndex < parameters.size) {
                    val parameter = parameters[positionalArgumentIndex]
                    map[argument] = parameter

                    if (!parameter.isVararg) {
                        positionalArgumentIndex++
                    }
                }
            }
        }
    }

    return map
}
