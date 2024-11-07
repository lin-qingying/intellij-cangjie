package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.psi.psiUtil.isFunctionalExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.StatementFilter
import com.linqingying.cangjie.resolve.TypeResolver
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver
import com.linqingying.cangjie.resolve.calls.components.CaseEnumArgument

import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.util.getCall
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil
import com.linqingying.cangjie.resolve.scopes.receivers.*
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo


// all arguments should be inherited from this class.
// But receivers is not, because for them there is no corresponding valueArgument
abstract class PSICangJieCallArgument : CangJieCallArgument {
    abstract val valueArgument: ValueArgument
    abstract val dataFlowInfoBeforeThisArgument: DataFlowInfo
    abstract val dataFlowInfoAfterThisArgument: DataFlowInfo

    override fun toString() =
        valueArgument.getArgumentExpression()?.text?.replace('\n', ' ') ?: valueArgument.toString()
}

val CangJieCallArgument.psiCallArgument: PSICangJieCallArgument
    get() {
        assert(this is PSICangJieCallArgument) {
            "Incorrect CangJieCallArgument: $this. Java class: ${javaClass.canonicalName}"
        }
        return this as PSICangJieCallArgument
    }

class CallableReferenceCangJieCallArgumentImpl(
    val scopeTowerForResolution: ImplicitScopeTower,
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val cjCallableReferenceExpression: CjCallableReferenceExpression,
    override val argumentName: Name?,
    override val lhsResult: LHSResult,
    override val rhsName: Name,
    override val call: CangJieCall
) : CallableReferenceCangJieCallArgument, PSICangJieCallArgument()

class CasePatternCangJieCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val argumentName: Name?,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val collectionLiteralExpression: CjCasePattern,
    val outerCallContext: BasicCallResolutionContext
) : CollectionLiteralCangJieCallArgument, PSICangJieCallArgument() {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
}

class CollectionLiteralCangJieCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val argumentName: Name?,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val collectionLiteralExpression: CjCollectionLiteralExpression,
    val outerCallContext: BasicCallResolutionContext
) : CollectionLiteralCangJieCallArgument, PSICangJieCallArgument() {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
}

class SubCangJieCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    override val receiver: ReceiverValueWithSmartCastInfo,
    override val callResult: PartialCallResolutionResult
) : SimplePSICangJieCallArgument(), SubCangJieCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class SimpleTypeArgumentImpl(
    val typeProjection: CjTypeProjection,
    override val type: UnwrappedType
) : SimpleTypeArgument

internal fun resolveType(
    context: BasicCallResolutionContext,
    typeReference: CjTypeReference?,
    typeResolver: TypeResolver
): UnwrappedType? {
    if (typeReference == null) return null
    return resolveType(context, typeReference, typeResolver)
}

@JvmName("resolveTypeWithGivenTypeReference")
internal fun resolveType(
    context: BasicCallResolutionContext,
    typeReference: CjTypeReference,
    typeResolver: TypeResolver
): UnwrappedType {
    val type = typeResolver.resolveType(context.scope, typeReference, context.trace, checkBounds = true)
    ForceResolveUtil.forceResolveAllContents(type)
    return type.unwrap()
}

internal fun CangJieCallArgument.setResultDataFlowInfoIfRelevant(resultDataFlowInfo: DataFlowInfo) {
//    if (this is PSIFunctionCangJieCallArgument) {
//        lambdaInitialDataFlowInfo = resultDataFlowInfo
//    }
}

abstract class SimplePSICangJieCallArgument : PSICangJieCallArgument(), SimpleCangJieCallArgument

// context here is context for value argument analysis
internal fun createSimplePSICallArgument(
    contextForArgument: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    typeInfoForArgument: CangJieTypeInfo
) = createSimplePSICallArgument(
    contextForArgument.trace.bindingContext, contextForArgument.statementFilter,
    contextForArgument.scope.ownerDescriptor, valueArgument,
    contextForArgument.dataFlowInfo, typeInfoForArgument,
    contextForArgument.languageVersionSettings,
    contextForArgument.dataFlowValueFactory,
    contextForArgument.call,
)

internal fun createSimplePSICallArgument(
    bindingContext: BindingContext,
    statementFilter: StatementFilter,
    ownerDescriptor: DeclarationDescriptor,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    typeInfoForArgument: CangJieTypeInfo,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory,
    call: Call
): SimplePSICangJieCallArgument? {
    val cjExpression =
        CjPsiUtil.getLastElementDeparenthesized(valueArgument.getArgumentExpression(), statementFilter) ?: return null
    val cjExpressionToExtractResolvedCall =
        if (cjExpression is CjCallableReferenceExpression) cjExpression.callableReference else cjExpression

    val partiallyResolvedCall = cjExpressionToExtractResolvedCall.getCall(bindingContext)?.let {
        bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)?.result
    }
    // todo hack for if expression: sometimes we not write properly type information for branches
    val baseType =
        typeInfoForArgument.type?.unwrap() ?: partiallyResolvedCall?.resultCallAtom?.freshReturnType ?: return null

    val expressionReceiver = ExpressionReceiver.create(cjExpression, baseType, bindingContext)
    val argumentWithSmartCastInfo =
        if (cjExpression is CjCallExpression || partiallyResolvedCall != null) {
            // For a sub-call (partially or fully resolved), there can't be any smartcast
            // so we use a fast-path here to avoid calling transformToReceiverWithSmartCastInfo function
            ReceiverValueWithSmartCastInfo(expressionReceiver, emptySet(), isStable = true)
        } else {
            val useDataFlowInfoBeforeArgument = call.callType == Call.CallType.CONTAINS
            transformToReceiverWithSmartCastInfo(
                ownerDescriptor, bindingContext,
                if (useDataFlowInfoBeforeArgument) dataFlowInfoBeforeThisArgument else typeInfoForArgument.dataFlowInfo,
                expressionReceiver,
//                languageVersionSettings,
                dataFlowValueFactory
            )
        }

    val capturedArgument = argumentWithSmartCastInfo.prepareReceiverRegardingCaptureTypes()

    return if (partiallyResolvedCall != null) {
        SubCangJieCallArgumentImpl(
            valueArgument,
            dataFlowInfoBeforeThisArgument,
            typeInfoForArgument.dataFlowInfo,
            capturedArgument,
            partiallyResolvedCall
        )
    } else {
        ExpressionCangJieCallArgumentImpl(
            valueArgument,
            dataFlowInfoBeforeThisArgument,
            typeInfoForArgument.dataFlowInfo,
            capturedArgument
        )
    }
}

class ExpressionCangJieCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    override val receiver: ReceiverValueWithSmartCastInfo
) : SimplePSICangJieCallArgument(), ExpressionCangJieCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class EmptyLabeledReturn(
    val returnExpression: CjReturnExpression,
    builtIns: CangJieBuiltIns
) : ExpressionCangJieCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = null
    override val receiver = ReceiverValueWithSmartCastInfo(TransientReceiver(builtIns.unitType), emptySet(), true)
    override val isSafeCall: Boolean get() = false
}

val CangJieCallArgument.psiExpression: CjExpression?
    get() {
        return when (this) {
            is CaseEnumArgument -> null
            is ReceiverExpressionCangJieCallArgument -> (receiver.receiverValue as? ExpressionReceiver)?.expression
            is QualifierReceiverCangJieCallArgument -> (receiver as? Qualifier)?.expression
            is EmptyLabeledReturn -> returnExpression
            else -> psiCallArgument.valueArgument.getArgumentExpression()
        }
    }

class ParseErrorCangJieCallArgument(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
) : ExpressionCangJieCallArgument, SimplePSICangJieCallArgument() {
    override val receiver = ReceiverValueWithSmartCastInfo(
        TransientReceiver(ErrorUtils.createErrorType(ErrorTypeKind.PARSE_ERROR_ARGUMENT, valueArgument.toString())),
        typesFromSmartCasts = emptySet(),
        isStable = true
    )

    override val isSafeCall: Boolean get() = false

    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName

    override val dataFlowInfoBeforeThisArgument: DataFlowInfo
        get() = dataFlowInfoAfterThisArgument
}

fun processFunctionalExpression(
    outerCallContext: BasicCallResolutionContext,
    argumentExpression: CjExpression,
    startDataFlowInfo: DataFlowInfo,
    valueArgument: ValueArgument,
    argumentName: Name?,
    builtIns: CangJieBuiltIns,
    typeResolver: TypeResolver
): PSICangJieCallArgument? {
    val expression =
        ArgumentTypeResolver.getFunctionLiteralArgumentIfAny(argumentExpression, outerCallContext) ?: return null
    val postponedExpression =
        if (expression is CjFunctionLiteral) expression.getParentOfType<CjLambdaExpression>(true) else expression

    val lambdaArgument: PSICangJieCallArgument = when (postponedExpression) {
        is CjLambdaExpression ->
            LambdaCangJieCallArgumentImpl(
                outerCallContext,
                valueArgument,
                startDataFlowInfo,
                argumentName,
                postponedExpression,
                argumentExpression,
                resolveParametersTypes(outerCallContext, postponedExpression.functionLiteral, typeResolver)
            )

        is CjNamedFunction -> {
            // if function is a not anonymous function, resolve it as simple expression
            if (!postponedExpression.isFunctionalExpression()) return null
            val receiverType = resolveType(outerCallContext, postponedExpression.receiverTypeReference, typeResolver)
            val contextReceiversTypes =
                resolveContextReceiversTypes(outerCallContext, postponedExpression, typeResolver)
            val parametersTypes =
                resolveParametersTypes(outerCallContext, postponedExpression, typeResolver) ?: emptyArray()
            val returnType = resolveType(outerCallContext, postponedExpression.typeReference, typeResolver)

            FunctionExpressionImpl(
                outerCallContext,
                valueArgument,
                startDataFlowInfo,
                argumentName,
                argumentExpression,
                postponedExpression,
                receiverType,
                contextReceiversTypes,
                parametersTypes,
                returnType
            )
        }

        else -> return null
    }

    checkNoSpread(outerCallContext, valueArgument)

    return lambdaArgument
}

abstract class PSIFunctionCangJieCallArgument(
    val outerCallContext: BasicCallResolutionContext,
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val argumentName: Name?
) : LambdaCangJieCallArgument, PSICangJieCallArgument() {
    override val dataFlowInfoAfterThisArgument: DataFlowInfo // todo drop this and use only lambdaInitialDataFlowInfo
        get() = dataFlowInfoBeforeThisArgument

    abstract val cjFunction: CjFunction
    abstract val expression: CjExpression
    lateinit var lambdaInitialDataFlowInfo: DataFlowInfo
}

class FunctionExpressionImpl(
    outerCallContext: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    argumentName: Name?,
    val containingBlockForFunction: CjExpression,
    override val cjFunction: CjNamedFunction,
    override val receiverType: UnwrappedType?,
    override val contextReceiversTypes: Array<UnwrappedType?>,
    override val parametersTypes: Array<UnwrappedType?>,
    override val returnType: UnwrappedType?
) : FunctionExpression,
    PSIFunctionCangJieCallArgument(outerCallContext, valueArgument, dataFlowInfoBeforeThisArgument, argumentName) {
    override val expression get() = containingBlockForFunction
}

private fun resolveParametersTypes(
    context: BasicCallResolutionContext,
    cjFunction: CjFunction,
    typeResolver: TypeResolver
): Array<UnwrappedType?>? {
    val parameterList = cjFunction.valueParameterList ?: return null

    return Array(parameterList.parameters.size) {
        parameterList.parameters[it].typeReference?.let { resolveType(context, it, typeResolver) }
    }
}

private fun resolveContextReceiversTypes(
    context: BasicCallResolutionContext,
    cjFunction: CjFunction,
    typeResolver: TypeResolver
): Array<UnwrappedType?> {
    val contextReceivers = cjFunction.contextReceivers

    return Array(contextReceivers.size) {
        contextReceivers[it].typeReference()?.let { typeRef -> resolveType(context, typeRef, typeResolver) }
    }
}

fun checkNoSpread(context: BasicCallResolutionContext, valueArgument: ValueArgument) {
    valueArgument.getSpreadElement()?.let {
        context.trace.report(Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE.on(it))
    }
}

class LambdaCangJieCallArgumentImpl(
    outerCallContext: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    argumentName: Name?,
    val cjLambdaExpression: CjLambdaExpression,
    val containingBlockForLambda: CjExpression,
    override val parametersTypes: Array<UnwrappedType?>?
) : PSIFunctionCangJieCallArgument(outerCallContext, valueArgument, dataFlowInfoBeforeThisArgument, argumentName) {
    override val cjFunction get() = cjLambdaExpression.functionLiteral
    override val expression get() = containingBlockForLambda

    override var hasBuilderInferenceAnnotation = false
        set(value) {
            assert(!field)
            field = value
        }

    override var builderInferenceSession: InferenceSession? = null
        set(value) {
            assert(field == null)
            field = value
        }
}
