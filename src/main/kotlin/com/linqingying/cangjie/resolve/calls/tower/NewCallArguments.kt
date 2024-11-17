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

package com.linqingying.cangjie.resolve.calls.tower

import com.intellij.psi.impl.source.tree.LeafPsiElement
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

/**
 * 创建一个简单的 PSI 调用参数对象
 *
 * 此函数负责根据给定的上下文和参数信息，构建一个简单的 PSI 调用参数对象
 * 它主要用于在解析调用过程中，为每个值参数创建相应的 PSI 节点
 *
 * @param contextForArgument 调用解析的上下文，包含跟踪信息、语句过滤器、作用域等
 * @param valueArgument 值参数，表示在调用中传递的实际参数
 * @param typeInfoForArgument 参数的类型信息，用于在创建 PSI 时确定参数的类型
 * @return 返回创建的简单 PSI 调用参数对象
 */
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
interface FakePositionalValueArgumentForCallableReference : ValueArgument {
    val index: Int
}
class FakePositionalValueArgumentForCallableReferenceImpl(
    private val callElement: CjElement,
    override val index: Int
) : FakePositionalValueArgumentForCallableReference {
    override fun getArgumentExpression(): CjExpression? = null
    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): CjElement = callElement
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = false
}

interface FakeImplicitSpreadValueArgumentForCallableReference : ValueArgument {
    val expression: ValueArgument
}
class FakeImplicitSpreadValueArgumentForCallableReferenceImpl(
    private val callElement: CjElement,
    override val expression: ValueArgument
) : FakeImplicitSpreadValueArgumentForCallableReference {
    override fun getArgumentExpression(): CjExpression? = null
    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): CjElement = callElement
    override fun getSpreadElement(): LeafPsiElement? = null // TODO callElement?
    override fun isExternal(): Boolean = false
}
class CallableReferenceCangJieCallArgumentImpl(
    val scopeTowerForResolution: ImplicitScopeTower,
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val cjCallableReferenceExpression: CjCallableReference,//CjCallableReferenceExpression
    override val argumentName: Name?,
    override val lhsResult: LHSResult,
    override val rhsName: Name,
    override val call: CangJieCall
) : CallableReferenceCangJieCallArgument, PSICangJieCallArgument()

class FakeValueArgumentForLeftCallableReference(val cjExpression: CjCallableReference) : ValueArgument {
    override fun getArgumentExpression() = cjExpression.receiverExpression

    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): CjElement = getArgumentExpression() ?: cjExpression
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = false
}

/**
 * 创建一个简单的 PSI 调用参数对象
 *
 * 此函数负责根据给定的参数和上下文信息，构建一个代表调用参数的 SimplePSICangJieCallArgument 实例
 * 它处理参数表达式的解析，类型信息的获取，以及根据条件构建相应的调用参数实现
 *
 * @param bindingContext 绑定上下文，用于获取解析信息
 * @param statementFilter 语句过滤器，用于筛选和处理表达式
 * @param ownerDescriptor 所有者描述符，表示参数所属的声明
 * @param valueArgument 值参数，表示调用中的实际参数
 * @param dataFlowInfoBeforeThisArgument 此参数之前的数据流信息
 * @param typeInfoForArgument 参数的类型信息
 * @param languageVersionSettings 语言版本设置，用于处理不同版本的 CangJie 语言特性
 * @param dataFlowValueFactory 数据流值工厂，用于创建数据流值
 * @param call 调用信息，表示参数所属的调用
 * @return 返回构建的 SimplePSICangJieCallArgument 实例，如果无法构建则返回 null
 */
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
    // 获取去除括号后的参数表达式，如果获取失败则返回 null
    val cjExpression =
        CjPsiUtil.getLastElementDeparenthesized(valueArgument.getArgumentExpression(), statementFilter) ?: return null

    // 确定用于提取解析调用的表达式
    val cjExpressionToExtractResolvedCall =
        if (cjExpression is CjCallableReferenceExpression) cjExpression.callableReference else cjExpression

    // 尝试获取部分解析的调用信息
    val partiallyResolvedCall = cjExpressionToExtractResolvedCall.getCall(bindingContext)?.let {
        bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)?.result
    }
    // todo 对 if 表达式的 hack：有时我们没有正确地为分支编写类型信息
    // 获取基础类型信息，如果获取失败则返回 null
    val baseType =
        typeInfoForArgument.type?.unwrap() ?: partiallyResolvedCall?.resultCallAtom?.freshReturnType ?: return null

    // 创建表达式接收器
    val expressionReceiver = ExpressionReceiver.create(cjExpression, baseType, bindingContext)

    // 根据条件处理接收器的智能类型转换信息
    val argumentWithSmartCastInfo =
        if (cjExpression is CjCallExpression || partiallyResolvedCall != null) {
            // 对于子调用（部分或完全解析的），不会有任何智能类型转换
// 因此我们在这里使用快速路径以避免调用 transformToReceiverWithSmartCastInfo 函数

            ReceiverValueWithSmartCastInfo(expressionReceiver, emptySet(), isStable = true)
        } else {
            val useDataFlowInfoBeforeArgument = call.callType == Call.CallType.CONTAINS
            transformToReceiverWithSmartCastInfo(
                ownerDescriptor, bindingContext,
                if (useDataFlowInfoBeforeArgument) dataFlowInfoBeforeThisArgument else typeInfoForArgument.dataFlowInfo,
                expressionReceiver,
                languageVersionSettings,
                dataFlowValueFactory
            )
        }

    // 准备接收器以处理捕获类型
    val capturedArgument = argumentWithSmartCastInfo.prepareReceiverRegardingCaptureTypes()

    // 根据是否解析了部分调用来决定返回的 SimplePSICangJieCallArgument 实现类型
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
