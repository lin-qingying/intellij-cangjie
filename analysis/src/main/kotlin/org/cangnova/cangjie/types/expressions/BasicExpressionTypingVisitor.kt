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

package org.cangnova.cangjie.types.expressions

import com.google.common.collect.Lists
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.cangnova.cangjie.builtins.BinaryOperatorRuleResultType
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isFloat
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isNothing
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isNumber
import org.cangnova.cangjie.builtins.CangJieBuiltIns.Companion.isUnit
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.InvalidBinaryData
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CjTokens.AS_KEYWORD
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.name.OperatorConventions.isConventionType
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjNodeTypes.INTEGER_CONSTANT
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.DescriptorUtils.isClass
import org.cangnova.cangjie.resolve.DescriptorUtils.isInterface
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.CAST_TYPE_USED_AS_EXPECTED_TYPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.COMPILE_TIME_VALUE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.INDEXED_LVALUE_GET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.INDEXED_LVALUE_SET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.RESOLVED_CALL
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.SUPER_EXPRESSION_FROM_ANY_MIGRATION
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.THIS_REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.THIS_TYPE_FOR_SUPER_EXPRESSION
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.VARIABLE_REASSIGNMENT
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isCallableReferenceArgument
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isCollectionLiteralArgument
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isFunctionLiteralArgument
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver.Companion.isFunctionLiteralOrCallableReference
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import org.cangnova.cangjie.resolve.calls.checkers.RttiExpressionInformation
import org.cangnova.cangjie.resolve.calls.checkers.RttiOperation
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue.Companion.nullValue
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.calls.util.CallMaker.makeCall
import org.cangnova.cangjie.resolve.constants.*
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.findFirstClassifierWithDeprecationStatus
import org.cangnova.cangjie.resolve.scopes.getImplicitReceiversHierarchy
import org.cangnova.cangjie.resolve.scopes.receivers.ContextReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver.Companion.create
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.ErrorUtils.invalidType
import org.cangnova.cangjie.types.ErrorUtils.isError
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.TypeUtils.makeNonOption
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import org.cangnova.cangjie.types.expressions.LabelResolver.LabeledReceiverResolutionResult.Companion.labelResolutionSuccess
import org.cangnova.cangjie.types.expressions.LabelResolver.resolveThisOrSuperLabel
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.errorTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.types.expressions.unqualifiedSuper.isPossiblyAmbiguousUnqualifiedSuper
import org.cangnova.cangjie.types.expressions.unqualifiedSuper.resolveUnqualifiedSuperFromExpressionContext
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isDynamic
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.isOptionType
import java.util.*
import org.cangnova.cangjie.types.TypeSubstitutor.Companion.create

/**
 * 基础表达式类型访问器
 *
 * 该类是表达式类型检查系统的核心组件，实现了访问者模式来处理各种类型的 PSI 表达式节点。
 * 它负责为仓颉语言中的所有基本表达式（包括二元表达式、一元表达式、调用表达式、字面量等）
 * 进行类型推导、类型检查和数据流分析。
 *
 * ## 主要职责
 *
 * 1. **类型推导**: 为各种表达式推导其类型，如字面量、运算符表达式、函数调用等
 * 2. **类型检查**: 验证表达式的类型是否符合预期类型和语言规范
 * 3. **运算符重载解析**: 处理运算符约定（operator conventions），解析重载的运算符
 * 4. **数据流分析**: 跟踪变量的可空性、智能类型转换等数据流信息
 * 5. **编译时常量求值**: 对常量表达式进行编译时求值
 * 6. **错误诊断**: 报告类型不匹配、不支持的操作等错误
 *
 * ## 处理的表达式类型
 *
 * - **二元表达式**: 算术运算、比较运算、逻辑运算、赋值运算等
 * - **一元表达式**: 前缀和后缀运算符（如 `++`, `--`, `!` 等）
 * - **调用表达式**: 函数调用、构造函数调用
 * - **字面量**: 整数、浮点数、字符串、布尔值等
 * - **数组访问**: `array[index]` 表达式及元组索引
 * - **限定表达式**: `a.b` 形式的成员访问
 * - **特殊表达式**: `this`, `super`, Elvis 运算符, 类型转换等
 *
 * ## 设计模式
 *
 * 该类使用访问者模式（Visitor Pattern），每个 `visitXXX` 方法对应一种表达式类型。
 * 这种设计使得类型检查逻辑与 PSI 树结构解耦，便于扩展和维护。
 *
 * ## 与其他组件的交互
 *
 * - 使用 [ExpressionTypingInternals] facade 来递归处理子表达式
 * - 通过 [ExpressionTypingComponents] 访问编译器的各种服务
 * - 使用 [BindingTrace] 记录类型信息和诊断错误
 * - 与 [CallResolver] 协作解析函数调用和运算符重载
 * - 与 [DataFlowAnalyzer] 协作进行数据流分析
 *
 * @param facade 表达式类型检查的门面接口，提供递归处理子表达式的能力
 *
 * @see ExpressionTypingVisitor
 * @see ExpressionTypingInternals
 * @see ExpressionTypingContext
 */
class BasicExpressionTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    /**
     * 检查 `in` 表达式
     *
     * 处理 `x in collection` 形式的包含性检查表达式。该操作符会被解析为
     * `collection.contains(x)` 方法调用。
     *
     * @param callElement 调用元素（整个 in 表达式）
     * @param operationSign in 操作符引用
     * @param leftArgument 左操作数（被检查的元素）
     * @param right 右操作数（集合表达式）
     * @param context 表达式类型检查上下文
     * @return 类型信息，通常应该是布尔类型
     */
    fun checkInExpression(
        callElement: CjElement,
        operationSign: CjSimpleNameExpression,
        leftArgument: ValueArgument,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val left = leftArgument.getArgumentExpression()
        val contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE)

        //        if (right == null) {
//            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
//            return TypeInfoFactoryCj.noTypeInfo(context);
//        }
        val rightTypeInfo = facade.getTypeInfo(right!!, contextWithNoExpectedType)
        val dataFlowInfo = rightTypeInfo.dataFlowInfo
        //
//        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
//        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);
//
//        Call containsCall = CallMaker.makeCall(
//                callElement, receiver, null, operationSign,
//                Collections.singletonList(leftArgument), Call.CallType.CONTAINS
//        );
//
//        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
//                contextWithDataFlow,
//                containsCall,
//                operationSign,
//                OperatorNameConventions.CONTAINS);
//        CangJieType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context);
//        ensureBooleanResult(operationSign, OperatorNameConventions.CONTAINS, containsType, context);
//
//        if (left != null) {
//            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
//            rightTypeInfo = rightTypeInfo.replaceDataFlowInfo(dataFlowInfo);
//        }
//        if (resolutionResult.isSuccess() || isResolutionSuccessfulWithOnlyInputTypesWarnings(resolutionResult.getResultingCalls(), context)) {
//            return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
//        } else {
        return rightTypeInfo.clearType()
        //        }
    }

    /**
     * 获取二元调用的类型信息
     *
     * 处理二元运算符表达式，将运算符解析为方法调用（运算符重载）。
     * 例如 `a + b` 会被解析为 `a.plus(b)` 方法调用。
     *
     * @param name 运算符对应的方法名（如 `plus`, `minus` 等）
     * @param context 表达式类型检查上下文
     * @param binaryExpression 二元表达式
     * @return 运算符调用的结果类型信息
     */
    private fun getTypeInfoForBinaryCall(
        name: Name,
        context: ExpressionTypingContext,
        binaryExpression: CjBinaryExpression
    ): CangJieTypeInfo {
        val left = binaryExpression.left
        val right = binaryExpression.right

        var typeInfo: CangJieTypeInfo
        typeInfo = if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            facade.getTypeInfo(
                left,
                context.replaceContextDependency(ContextDependency.INDEPENDENT)
                    .replaceExpectedType(NO_EXPECTED_TYPE)
            )
        } else {
            noTypeInfo(context)
        }

        val contextWithDataFlow = context.replaceDataFlowInfo(typeInfo.dataFlowInfo)

        val resolutionResults: OverloadResolutionResults<FunctionDescriptor>
        if (left != null) {
            val receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context)
            resolutionResults =
                components.callResolver.resolveBinaryCall(contextWithDataFlow, receiver, binaryExpression, name)
        } else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound()
        }

        if (resolutionResults.isSingleResult) {
            typeInfo =
                typeInfo.replaceDataFlowInfo(resolutionResults.resultingCall.dataFlowInfoForArguments.resultInfo)
        }

        //        if (OverloadResolutionResultsUtil.getResultingType(resolutionResults, context) == null) {
//            if (right != null) {
//                CjSimpleNameExpression operationSign = binaryExpression.getOperationReference();
//                IElementType operationType = operationSign.getReferencedNameElementType();
//
//                CangJieTypeInfo rightInfo = facade.getTypeInfo(right
//                        , context.replaceContextDependency(ContextDependency.INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
//                context.trace.report(INVALID_BINARY_OPERATOR.on(operationSign, new InvalidBinaryData(
//                        ((CjSingleValueToken) operationType).getValue(), typeInfo.getType(), rightInfo.getType()
//                )));
//            }
//        }
        return typeInfo.replaceType(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context))
    }

    /**
     * 赋值不能作为表达式错误
     *
     * 在仓颉语言中，赋值语句不能作为表达式使用（例如不能写 `if (x = 5)`）。
     * 该方法检查赋值表达式并报告相应错误。
     *
     * @param expression 二元赋值表达式
     * @param context 表达式类型检查上下文
     * @return 无类型信息（赋值不产生值）
     */
    private fun assignmentIsNotAnExpressionError(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        facade.checkStatementType(expression, context)
        if (!context.isDebuggerContext && context.isSaveTypeInfo) {
            context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression))
        }
        return noTypeInfo(context)
    }

    /**
     * 访问数组访问表达式
     *
     * 处理数组索引操作，如 `array[index]` 或元组索引 `tuple[0]`。
     * 该操作会被解析为 `get` 方法调用（对于数组）或直接类型提取（对于元组）。
     *
     * @param expression 数组访问表达式
     * @param context 表达式类型检查上下文
     * @return 数组元素或元组元素的类型信息
     */
    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.dataFlowAnalyzer.checkType(
            resolveArrayAccessGetMethod(expression, context),
            expression,
            context
        )
    }

    /**
     * 解析元组的数组访问
     *
     * 特殊处理元组类型的索引访问，要求索引必须是整数字面量且在合法范围内。
     * 元组索引在编译时确定，因此必须是常量。
     *
     * 检查内容：
     * 1. 索引数量必须为 1
     * 2. 索引必须是整数字面量
     * 3. 索引不能越界
     *
     * @param type 元组类型
     * @param expression 数组访问表达式
     * @param oldContext 旧的表达式类型检查上下文
     * @param traceForResolveResult 用于记录解析结果的绑定跟踪
     * @param isGet 是否为 get 操作（true）或 set 操作（false）
     * @param isImplicit 是否为隐式操作
     * @return 元组对应索引位置的元素类型信息
     */
    private fun resolveArrayAccessForTuple(
        type: CangJieType,
        expression: CjArrayAccessExpression,
        oldContext: ExpressionTypingContext,
        traceForResolveResult: BindingTrace,
        isGet: Boolean,
        isImplicit: Boolean
    ): CangJieTypeInfo {
//        检查index元素数量，如果不是1，那么是扩展方法，直接报错
//        检查psi元素类型，必须是integer 字面量
//        检查越界情况
//        返回值：元组对应的元素类型

        val indexElements = expression.indexExpressions
        val indices = indexElements.map {
            facade.getTypeInfo(it, oldContext.replaceExpectedType(components.builtIns.int64Type)).type
        }
        if (indexElements.size > 1) {
            traceForResolveResult.report(
                if (isGet) NO_GET_FOR_TUPLE_METHOD.on(expression) else NO_SET_FOR_TUPLE_METHOD.on(
                    expression
                )
            )
            return noTypeInfo(oldContext)
        }
        val indexElement = indexElements.singleOrNull() ?: return noTypeInfo(oldContext)
        val indexType = indices.firstOrNull() ?: return noTypeInfo(oldContext)


        val elementType = indexElement.elementType

        if (elementType != INTEGER_CONSTANT) {
            traceForResolveResult.report(
                NON_INTEGER_TUPLE_INDEX.on(
                    indexElement
                )
            )
            return noTypeInfo(oldContext)
        }
//        检查越界情况
        val compileValue = traceForResolveResult[COMPILE_TIME_VALUE, indexElement]
        if (compileValue == null) {
            traceForResolveResult.report(
                NON_INTEGER_TUPLE_INDEX.on(
                    indexElement
                )
            )
            return noTypeInfo(oldContext)
        }
        val index = compileValue.getValue(indexType) as? Long
        if (index == null) {
            traceForResolveResult.report(
                NON_INTEGER_TUPLE_INDEX.on(
                    indexElement
                )
            )
            return noTypeInfo(oldContext)
        }
        val indexByInt = index.toInt()
        if (indexByInt >= type.arguments.size || indexByInt < 0) {
            traceForResolveResult.report(
                TUPLE_INDEX_OUT_OF_RANGE.on(
                    indexElement,
                    /*    index,
                        type.arguments.size*/
                )
            )
            return noTypeInfo(oldContext)

        }
        return if (isGet) {
            createTypeInfo(type.arguments[indexByInt].type)
        } else {
            noTypeInfo(oldContext)
        }


    }

    private fun resolveArrayAccessSpecialMethod(
        arrayAccessExpression: CjArrayAccessExpression,
        rightHandSide: CjExpression?, // only for 'set' method
        oldContext: ExpressionTypingContext,
        traceForResolveResult: BindingTrace,
        isGet: Boolean,
        isImplicit: Boolean
    ): CangJieTypeInfo {


        val arrayExpression = arrayAccessExpression.arrayExpression ?: return noTypeInfo(oldContext)

        val arrayTypeInfo = facade.safeGetTypeInfo(
            arrayExpression,
            oldContext.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
        )
        val arrayType = ExpressionTypingUtils.safeGetType(arrayTypeInfo)


        val context = oldContext.replaceDataFlowInfo(arrayTypeInfo.dataFlowInfo)
        val receiver = create(arrayExpression, arrayType, context.trace.bindingContext)
        if (!isGet) assert(rightHandSide != null)

        val call = if (isGet) {
            CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
        } else {
            CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide!!, Call.CallType.ARRAY_SET_METHOD)
        }
        val ftrace = TemporaryBindingTrace.create(oldContext.trace, "resolve array access special method")
        val functionContext = context.replaceBindingTrace(ftrace)
        val functionResults = components.callResolver.resolveCallWithGivenName(
            functionContext,
            call,
            arrayAccessExpression,

            if (isGet) OperatorNameConventions.GET else OperatorNameConventions.SET

        )

        val indices = arrayAccessExpression.indexExpressions

        val resultTypeInfo = computeAccumulatedInfoForArrayAccessExpression(
            arrayTypeInfo,
            indices,
            rightHandSide,
            isGet,
            context,
            facade
        )

        if ((isImplicit && !functionResults.isSuccess) || !functionResults.isSingleResult) {

            if (arrayType.isBuiltinTupleType) {
                return resolveArrayAccessForTuple(
                    arrayType,
                    arrayAccessExpression,
                    oldContext,
                    traceForResolveResult,
                    isGet,
                    isImplicit
                )
            }
            traceForResolveResult.report(
                if (isGet) NO_GET_METHOD.on(arrayAccessExpression) else NO_SET_METHOD.on(
                    arrayAccessExpression
                )
            )
            ftrace.commit()
            return resultTypeInfo.clearType()
        }
        ftrace.commit()

        if (isGet) {
            traceForResolveResult.record(INDEXED_LVALUE_GET, arrayAccessExpression, functionResults.resultingCall)
        } else {
            traceForResolveResult.record(INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.resultingCall)
        }

        return resultTypeInfo.replaceType(functionResults.resultingDescriptor.returnType)
    }

    private fun checkForCastImpossibilityOrRedundancy(
        expression: CjBinaryExpressionWithTypeRHS,
        actualType: CangJieType?,
        targetType: CangJieType,
        context: ExpressionTypingContext
    ) {
        if (actualType == null || noExpectedType(targetType) || targetType.isError) return

        if (context.trace[CAST_TYPE_USED_AS_EXPECTED_TYPE, expression] == true) return

        if (targetType.isDynamic()) {
            val right = expression.right
            requireNotNull(right) { "We know target is dynamic, but RHS is missing" }
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(right))
            return
        }

//        if (!actualType.isStubType() &&
//            !CastDiagnosticsUtil.isCastPossible(
//                actualType,
//                targetType,
//                components.platformToCangJieClassMapper,
////                components.platformSpecificCastChecker
//            )
//        ) {
//            context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.operationReference))
//            return
//        }
//
//        if (CastDiagnosticsUtil.castIsUseless(expression, context, targetType, actualType)) {
//            context.trace.report(USELESS_CAST.on(expression))
//            return
//        }
//
//        if (CastDiagnosticsUtil.isCastErased(actualType, targetType, CangJieTypeChecker.DEFAULT)) {
//            context.trace.report(UNCHECKED_CAST.on(expression, actualType, targetType))
//        }
    }

    private fun checkBinaryWithTypeRHS(
        expression: CjBinaryExpressionWithTypeRHS,
        context: ExpressionTypingContext,
        targetType: CangJieType,
        actualType: CangJieType?
    ) {
        if (actualType == null) return
        val operationSign = expression.operationReference
        val operationType = operationSign.referencedNameElementType
        if (operationType != AS_KEYWORD) {
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"))
            return
        }
        checkForCastImpossibilityOrRedundancy(expression, actualType, targetType, context)
//        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperCheckAnnotationsTargetInTypeUsePositions)) {
//            components.annotationChecker.check(expression.right, context.trace, null)
//        }
    }

    override fun visitBinaryWithTypeRHSExpression(
        expression: CjBinaryExpressionWithTypeRHS,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val contextWithNoExpectedType: ExpressionTypingContext =
            context.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)
        val left: CjExpression = expression.left
        val right: CjTypeReference = expression.right
            ?: return facade.getTypeInfo(left, contextWithNoExpectedType).clearType()

        expression.reportDeprecatedDefinitelyNotNullSyntax(right, context)

        val operationType: IElementType = expression.operationReference.referencedNameElementType

        val allowBareTypes: Boolean =
            BARE_TYPES_ALLOWED.contains(
                operationType
            )
        val typeResolutionContext =
            TypeResolutionContext(
                context.scope,
                context.trace,
                true,
                allowBareTypes,
                context.isDebuggerContext
            )
        val possiblyBareTarget: PossiblyBareType =
            components.typeResolver.resolvePossiblyBareType(typeResolutionContext, right)

        var typeInfo: CangJieTypeInfo = facade.getTypeInfo(left, contextWithNoExpectedType)

        val subjectType = typeInfo.type
        val targetType: CangJieType =
            TypeReconstructionUtil.reconstructBareType(
                right,
                possiblyBareTarget,
                subjectType,
                context.trace,
                components.builtIns
            )

        if (subjectType != null) {
            checkBinaryWithTypeRHS(expression, context, targetType, subjectType)
            val dataFlowInfo = typeInfo.dataFlowInfo
            if (operationType === AS_KEYWORD) {
                val value =
                    components.dataFlowValueFactory.createDataFlowValue(left, subjectType, context)
                typeInfo = typeInfo.replaceDataFlowInfo(
                    dataFlowInfo.establishSubtyping(
                        value, targetType,
                        components.languageVersionSettings
                    )
                )
            }
        }

        val result =
            TypeUtils.makeOptional(
                targetType
            )
        val resultTypeInfo: CangJieTypeInfo =
            components.dataFlowAnalyzer.checkType(typeInfo.replaceType(result), expression, context)

        val rttiInformation =
            RttiExpressionInformation(
                expression.left,
                subjectType,
                result,
                RttiOperation.AS
            )
        for (checker in components.rttiExpressionCheckers) {
            checker.check(rttiInformation, expression, context.trace)
        }

        return resultTypeInfo
    }

    fun resolveArrayAccessGetMethod(
        arrayAccessExpression: CjArrayAccessExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true, false)
    }

    fun resolveArrayAccessSetMethod(
        arrayAccessExpression: CjArrayAccessExpression,
        rightHandSide: CjExpression,
        context: ExpressionTypingContext,
        traceForResolveResult: BindingTrace
    ): CangJieTypeInfo {
        return resolveArrayAccessSpecialMethod(
            arrayAccessExpression,
            rightHandSide,
            context,
            traceForResolveResult,
            isGet = false,
            isImplicit = false
        )
    }

    override fun visitBlockExpression(
        expression: CjBlockExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, false)
    }

    private fun visitAssignment(expression: CjBinaryExpression, context: ExpressionTypingContext): CangJieTypeInfo {
//        return assignmentIsNotAnExpressionError(expression, context)

        return facade.getTypeInfo(expression, context, true)

        return createTypeInfo(components.builtIns.unitType)
    }

    fun operatorOverloading(
        operationType: IElementType?,
        context: ExpressionTypingContext?,
        expression: CjBinaryExpression?
    ): CangJieTypeInfo? {
        val result: CangJieTypeInfo? = null


        return result
    }

    private fun visitCoalescingExpression(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
//        return visitElvisExpression(expression,contextWithExpectedType)
        // 替换上下文中的预期类型为 "无预期类型"
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
        val left = expression.left
        val right = expression.right

        // 如果左操作数或右操作数为空，返回空类型信息
        if (left == null || right == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade)
            return noTypeInfo(context)
        }

        // 为 Elvis 表达式创建特殊的调用结构
        val call = createCallForSpecialConstruction(
            expression,
            expression.operationReference,
            Lists.newArrayList(left, right)
        )
        // 解析特殊构造（Elvis 操作符）的调用
        val resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
            call,
            ControlStructureTypingUtils.ResolveConstruct.COALESCING,
            Lists.newArrayList("left", "right"),
            Lists.newArrayList(true, false),
            contextWithExpectedType,
            null
        )
        val leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.bindingContext)
        val isLeftFunctionLiteral = isFunctionLiteralArgument(left, context)
        val isLeftCallableReference = isCallableReferenceArgument(left, context)
        val isLeftCollectionLiteral = isCollectionLiteralArgument(left)

        // 如果左侧表达式的类型信息为空且是某些特殊类型的表达式（如函数字面值、可调用引用或集合字面值），则返回空类型信息
        if (leftTypeInfo == null && (isLeftFunctionLiteral || isLeftCallableReference || isLeftCollectionLiteral)) {
            return noTypeInfo(context)
        }

        // 确保左侧表达式的类型信息不为空
        checkNotNull(leftTypeInfo) { "Left expression was not processed: $expression" }

        val leftType = leftTypeInfo.type
        val rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.bindingContext)

        // 如果右侧是函数字面值或可调用引用，但类型信息为空，延后处理
        if (rightTypeInfo == null && isFunctionLiteralOrCallableReference(right, context)) {
            return noTypeInfo(context)
        }

        // 确保右侧表达式的类型信息不为空
        checkNotNull(rightTypeInfo) { "Right expression was not processed: $expression" }

        val loopBreakContinuePossible = leftTypeInfo.jumpOutPossible || rightTypeInfo.jumpOutPossible
        val rightType = rightTypeInfo.type

        // 仅考虑左操作数的数据流分析，因为不能确定右操作数是否已合并
        var dataFlowInfo = resolvedCall.dataFlowInfoForArguments.getInfo(call.valueArguments[1])

        var type = resolvedCall.resultingDescriptor.returnType
        // 如果返回类型、右侧类型或左侧类型为空且右侧类型为 `Nothing`，则返回空类型信息
        if (type == null || rightType == null || leftType == null && isNothing(rightType)) {
            return noTypeInfo(dataFlowInfo)
        }

        if (leftType != null) {
            val leftValue = components.dataFlowValueFactory.createDataFlowValue(left, leftType, context)
            var rightDataFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            val jumpInRight = isNothing(rightType)
            val nullValue = nullValue(components.builtIns)

            // 如果右侧为空或在右侧数据流中左值不能为 null，则更新数据流信息
            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue, components.languageVersionSettings)
                // 如果左操作数是带有类型 RHS 的二元表达式，进一步建立子类型关系
                if (left is CjBinaryExpressionWithTypeRHS) {
                    dataFlowInfo = establishSubtypingForTypeRHS(
                        left,
                        dataFlowInfo,
                        context,
                        components.languageVersionSettings
                    )
                }
            }
            val resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context)
            dataFlowInfo = dataFlowInfo
                .assign(resultValue, leftValue)
                .disequate(resultValue, nullValue, components.languageVersionSettings)

            // 如果右侧没有跳转，将右侧值合并到数据流
            if (!jumpInRight) {
                val rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context)
                rightDataFlowInfo = rightDataFlowInfo.assign(resultValue, rightValue)
                dataFlowInfo = dataFlowInfo.or(rightDataFlowInfo)
            }
        }

        // 如果右侧类型不可空，但结果类型可空，强制将结果类型设为不可空
        if (!isOptionType(rightType) && isOptionType(type)) {
            type = makeNonOption(type)
        }

        // 如果上下文依赖是 "依赖上下文"，返回类型信息
        if (context.contextDependency == ContextDependency.DEPENDENT) {
            return createTypeInfo(type, dataFlowInfo)
        }

        // 如果可能存在跳转，则使用条件检查信息作为跳转信息
        return createTypeInfo(
            components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
            dataFlowInfo,
            loopBreakContinuePossible,
            context.dataFlowInfo
        )
    }


    private fun visitElvisExpression(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        // 替换上下文中的预期类型为 "无预期类型"
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
        val left = expression.left
        val right = expression.right

        // 如果左操作数或右操作数为空，返回空类型信息
        if (left == null || right == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade)
            return noTypeInfo(context)
        }

        // 为 Elvis 表达式创建特殊的调用结构
        val call = createCallForSpecialConstruction(
            expression,
            expression.operationReference,
            Lists.newArrayList(left, right)
        )
        // 解析特殊构造（Elvis 操作符）的调用
        val resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
            call,
            ControlStructureTypingUtils.ResolveConstruct.ELVIS,
            Lists.newArrayList("left", "right"),
            Lists.newArrayList(true, false),
            contextWithExpectedType,
            null
        )
        val leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.bindingContext)
        val isLeftFunctionLiteral = isFunctionLiteralArgument(left, context)
        val isLeftCallableReference = isCallableReferenceArgument(left, context)
        val isLeftCollectionLiteral = isCollectionLiteralArgument(left)

        // 如果左侧表达式的类型信息为空且是某些特殊类型的表达式（如函数字面值、可调用引用或集合字面值），则返回空类型信息
        if (leftTypeInfo == null && (isLeftFunctionLiteral || isLeftCallableReference || isLeftCollectionLiteral)) {
            return noTypeInfo(context)
        }

        // 确保左侧表达式的类型信息不为空
        checkNotNull(leftTypeInfo) { "Left expression was not processed: $expression" }

        val leftType = leftTypeInfo.type
        val rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.bindingContext)

        // 如果右侧是函数字面值或可调用引用，但类型信息为空，延后处理
        if (rightTypeInfo == null && isFunctionLiteralOrCallableReference(right, context)) {
            return noTypeInfo(context)
        }

        // 确保右侧表达式的类型信息不为空
        checkNotNull(rightTypeInfo) { "Right expression was not processed: $expression" }

        val loopBreakContinuePossible = leftTypeInfo.jumpOutPossible || rightTypeInfo.jumpOutPossible
        val rightType = rightTypeInfo.type

        // 仅考虑左操作数的数据流分析，因为不能确定右操作数是否已合并
        var dataFlowInfo = resolvedCall.dataFlowInfoForArguments.getInfo(call.valueArguments[1])

        var type = resolvedCall.resultingDescriptor.returnType
        // 如果返回类型、右侧类型或左侧类型为空且右侧类型为 `Nothing`，则返回空类型信息
        if (type == null || rightType == null || leftType == null && isNothing(rightType)) {
            return noTypeInfo(dataFlowInfo)
        }

        if (leftType != null) {
            val leftValue = components.dataFlowValueFactory.createDataFlowValue(left, leftType, context)
            var rightDataFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            val jumpInRight = isNothing(rightType)
            val nullValue = nullValue(components.builtIns)

            // 如果右侧为空或在右侧数据流中左值不能为 null，则更新数据流信息
            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue, components.languageVersionSettings)
                // 如果左操作数是带有类型 RHS 的二元表达式，进一步建立子类型关系
                if (left is CjBinaryExpressionWithTypeRHS) {
                    dataFlowInfo = establishSubtypingForTypeRHS(
                        left,
                        dataFlowInfo,
                        context,
                        components.languageVersionSettings
                    )
                }
            }
            val resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context)
            dataFlowInfo = dataFlowInfo
                .assign(resultValue, leftValue)
                .disequate(resultValue, nullValue, components.languageVersionSettings)

            // 如果右侧没有跳转，将右侧值合并到数据流
            if (!jumpInRight) {
                val rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context)
                rightDataFlowInfo = rightDataFlowInfo.assign(resultValue, rightValue)
                dataFlowInfo = dataFlowInfo.or(rightDataFlowInfo)
            }
        }

        // 如果右侧类型不可空，但结果类型可空，强制将结果类型设为不可空
        if (!isOptionType(rightType) && isOptionType(type)) {
            type = makeNonOption(type)
        }

        // 如果上下文依赖是 "依赖上下文"，返回类型信息
        if (context.contextDependency == ContextDependency.DEPENDENT) {
            return createTypeInfo(type, dataFlowInfo)
        }

        // 如果可能存在跳转，则使用条件检查信息作为跳转信息
        return createTypeInfo(
            components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
            dataFlowInfo,
            loopBreakContinuePossible,
            context.dataFlowInfo
        )
    }

    private fun establishSubtypingForTypeRHS(
        left: CjBinaryExpressionWithTypeRHS,
        dataFlowInfo: DataFlowInfo,
        context: ExpressionTypingContext,
        languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        val operationType = left.operationReference.referencedNameElementType
//        if (operationType == AS_SAFE) {
//            val underSafeAs = left.left
//            val underSafeAsType = context.trace.getType(underSafeAs)
//            if (underSafeAsType != null) {
//                val underSafeAsValue = components.dataFlowValueFactory.createDataFlowValue(underSafeAs, underSafeAsType, context)
//                val targetType = context.trace.get(BindingContext.TYPE, left.right)
//                if (targetType != null) {
//                    return dataFlowInfo.establishSubtyping(underSafeAsValue, targetType, languageVersionSettings)
//                }
//            }
//        }
        return dataFlowInfo
    }

    private fun visitComparison(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext,
        operationSign: CjSimpleNameExpression
    ): CangJieTypeInfo {
        val operationType = operationSign.referencedNameElementType

        val referencedName = OperatorConventions.COMPARISON_OPERATIONS_NAMES[operationType]

        return getTypeInfoForBinaryCall(referencedName!!, context, expression)
    }

    fun visitFlowOperationExpression(
        operationType: IElementType?,
        left: CjExpression?,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {

//        流入 和 组合

        val leftTypeInfo =
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade)




        TODO()
    }

    fun visitBooleanOperationExpression(
        operationType: IElementType?,
        left: CjExpression?,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val booleanType: CangJieType = components.builtIns.boolType
        val leftTypeInfo =
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade)
        val dataFlowInfo = leftTypeInfo.dataFlowInfo

        val leftScope = ExpressionTypingUtils.newWritableScopeImpl(
            context,
            LexicalScopeKind.LEFT_BOOLEAN_EXPRESSION,
            facade.components.overloadChecker
        )
        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
        val isAnd = operationType === CjTokens.ANDAND
        val flowInfoLeft =
            components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo)
        val rightScope = if (isAnd) leftScope else ExpressionTypingUtils.newWritableScopeImpl(
            context, LexicalScopeKind.RIGHT_BOOLEAN_EXPRESSION,
            facade.components.overloadChecker
        )

        val contextForRightExpr =
            context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType)
        if (right != null) {
            facade.getTypeInfo(right, contextForRightExpr)
        }
        return leftTypeInfo.replaceType(booleanType)
    }

    override fun visitRangeExpression(expression: CjRangeExpression, data: ExpressionTypingContext): CangJieTypeInfo {


        return components.rangeLiteralResolver.resolveRangeLiteral(expression, data)
    }

    /**
     * 访问二元表达式
     *
     * 这是处理所有二元运算符表达式的核心方法，包括：
     * - 算术运算符: `+`, `-`, `*`, `/`, `%`
     * - 比较运算符: `<`, `>`, `<=`, `>=`, `==`, `!=`
     * - 逻辑运算符: `&&`, `||`
     * - 赋值运算符: `=`, `+=`, `-=` 等
     * - 特殊运算符: Elvis (`??`), 流操作符等
     *
     * 该方法会根据运算符类型分发到相应的处理逻辑。
     *
     * @param expression 二元表达式
     * @param contextWithExpectedType 包含预期类型的表达式类型检查上下文
     * @return 二元表达式的结果类型信息
     */
    override fun visitBinaryExpression(
        expression: CjBinaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = if (ExpressionTypingUtils.isBinaryExpressionDependentOnExpectedType(expression))
            contextWithExpectedType
        else
            contextWithExpectedType.replaceContextDependency(ContextDependency.INDEPENDENT)
                .replaceExpectedType(NO_EXPECTED_TYPE)

        val operationSign: CjSimpleNameExpression = expression.operationReference
        val left = expression.left
        val right = expression.right
        val operationType = operationSign.referencedNameElementType

        val result: CangJieTypeInfo

        if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            val referencedName = OperatorConventions.BINARY_OPERATION_NAMES[operationType]
            result = getTypeInfoForBinaryCall(referencedName!!, context, expression)
        } else if (operationType === CjTokens.COALESCING) {
            //base expression of elvis operator is checked for 'type mismatch', so the whole expression shouldn't be checked
            return visitCoalescingExpression(expression, context)
        } else if (OperatorConventions.COMPARISON_OPERATIONS_NAMES.containsKey(operationType)) {
            result = visitComparison(expression, context, operationSign)
        } else if (operationType === CjTokens.EQ) {
            result = visitAssignment(expression, context)
        } else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context)
        } else if (OperatorConventions.BOOLEAN_OPERATIONS_NAMES.containsKey(operationType)) {
            result = visitBooleanOperationExpression(operationType, left, right, context)
        } else if (OperatorConventions.FLOW_OPERATION_NAMES.containsKey(operationType)) {
            val referencedName = OperatorConventions.FLOW_OPERATION_NAMES[operationType]
//            result = getTypeInfoForBinaryCall(referencedName!!, context, expression)
//            result = visitFlowOperationExpression(operationType, left, right, context)
            result = components.flowOperatorResolver.resolveFlowOperator(referencedName!!, expression, context)
        } else {
            context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"))
            result = noTypeInfo(context)
        }

        val value = components.constantExpressionEvaluator.evaluateExpression(
            expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        )
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(
                value,
                expression,
                contextWithExpectedType
            )
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType)
    }

    private fun visitAssignmentOperation(
        expression: CjBinaryExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return assignmentIsNotAnExpressionError(expression, context)
    }

    private fun checkOperatorByType(
        leftTypeInfo: CangJieTypeInfo, rightTypeInfo: CangJieTypeInfo,
        operationSign: CjSimpleNameExpression, context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val operationType = operationSign.referencedNameElementType


        if (leftTypeInfo.type is ErrorType) {
            return leftTypeInfo
        }

        val result =
            components.builtIns.matchBinaryOperatorRule(operationType as CjToken, leftTypeInfo.type, rightTypeInfo.type)

        when (result.resultType) {
            BinaryOperatorRuleResultType.LEFT -> {
                return leftTypeInfo
            }

            BinaryOperatorRuleResultType.RIGHT -> {
                return rightTypeInfo
            }

            BinaryOperatorRuleResultType.ERROR -> {
                if (isConventionType(operationType)) {
                    context.trace.report(
                        INVALID_BINARY_OPERATOR.on(
                            operationSign, InvalidBinaryData(
                                (operationType as CjSingleValueToken).value, leftTypeInfo.type!!, rightTypeInfo.type!!
                            )
                        )
                    )
                } else {
//                    不可被重载的操作符报告错误
                    throw UnsupportedOperationException("Unsupported operator: $operationType")
                }
            }
        }
        return errorTypeInfo(invalidType, context)
    }

    private fun checkOperatorByType(operationSign: CjSimpleNameExpression, type: CangJieType): Boolean {
        val operationType = operationSign.referencedNameElementType

        //        int类型
        return if (isNumber(type) && CjTokens.INT_SUPPORT_OPERATOR.contains(operationType)) {
            true
        } else isFloat(type) && CjTokens.FLOAT_SUPPORT_OPERATOR.contains(operationType)
    }

    /**
     * 访问调用表达式
     *
     * 处理函数调用表达式，如 `foo()`, `obj.method(args)` 等。
     * 该方法会解析函数引用、匹配参数类型、进行重载解析等。
     *
     * 特殊处理：
     * - 如果调用名称为 "VArray"，会使用特殊的 VArray 解析器
     *
     * @param expression 调用表达式
     * @param data 表达式类型检查上下文
     * @return 函数调用的返回类型信息
     */
    override fun visitCallExpression(
        expression: CjCallExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {
        if (expression.referenceExpression?.referencedName == "VArray") {
            return facade.components.vArrayResolver.resolve(expression, data)
        }
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getCallExpressionTypeInfo(expression, data)
    }

    private fun checkNull(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext,
        type: CangJieType?
    ) {
    }

    /**
     * 获取常量类型的默认类型
     *
     * 根据常量字面量的类型返回对应的默认类型。
     * 例如整数字面量默认为 Int64，浮点数字面量默认为 Float64。
     *
     * @param constantType 常量的元素类型
     * @return 对应的默认仓颉类型
     * @throws IllegalArgumentException 如果常量类型不受支持
     */
    fun getDefaultType(constantType: IElementType): CangJieType {
        val builtIns = components.builtIns
        return if (constantType === INTEGER_CONSTANT) {
            builtIns.int64Type
        } else if (constantType === CjNodeTypes.FLOAT_CONSTANT) {
            builtIns.float64Type
        } else if (constantType === CjNodeTypes.BOOLEAN_CONSTANT) {
            builtIns.boolType
        } else if (constantType === CjNodeTypes.RUNE_CONSTANT) {
            builtIns.runeType
        } else {
            throw IllegalArgumentException("Unsupported constant type: $constantType")
        }
    }

    /**
     * 检查字面量中的下划线
     *
     * @param elementType
     */
    private fun checkUnderscores(
        expression: CjConstantExpression,
        elementType: IElementType,
        context: ExpressionTypingContext
    ) {
        val text = expression.text.lowercase(Locale.getDefault())

        if (!text.contains("_")) return

        //        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.UnderscoresInNumericLiterals)) {
//            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(expression,
//                    TuplesCj.to(LanguageFeature.UnderscoresInNumericLiterals, components.languageVersionSettings)));
//            return;
//        }
        if (hasIllegalUnderscore(expression.text, elementType)) {
            context.trace.report(ILLEGAL_UNDERSCORE.on(expression))
        }
    }

    /**
     * 访问字符串模板表达式
     *
     * 处理字符串字面量和字符串模板，包括普通字符串和包含嵌入表达式的字符串。
     * 例如：`"Hello"` 或 `"Hello, ${name}!"`
     *
     * 该方法会：
     * 1. 检查字符串前缀和后缀的合法性
     * 2. 处理字符串模板中的嵌入表达式
     * 3. 检查转义序列的正确性
     * 4. 尝试进行编译时常量求值
     *
     * @param expression 字符串模板表达式
     * @param contextWithExpectedType 包含预期类型的表达式类型检查上下文
     * @return String 类型的类型信息
     */
    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        checkLiteralPrefixAndSuffix(expression, context)

        class StringTemplateVisitor : CjVisitorUnit() {
            var typeInfo: CangJieTypeInfo = noTypeInfo(context)

            override fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression) {
                val entryExpression = entry.expression
                if (entryExpression != null) {
                    val toString = context.scope.findFirstClassifierWithDeprecationStatus(
                        StandardNames.TOSTRING,
                        NoLookupLocation.FROM_BUILTINS
                    )

                    typeInfo = if (toString != null) {
                        //                  约束toString类型
                        facade.getTypeInfo(
                            entryExpression,
                            context.replaceExpectedType(toString.descriptor.defaultType)
                                .replaceDataFlowInfo(typeInfo.dataFlowInfo)
                        )
                    } else {
                        facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(typeInfo.dataFlowInfo))
                    }
                }
            }

            override fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry) {
                val value =
                    CompileTimeConstantChecker.escapedStringToCharacter(entry.text, entry)
                val diagnostic = value.diagnostic
                if (diagnostic != null) {
                    context.trace.report(diagnostic)
                }
            }
        }

        val visitor = StringTemplateVisitor()
        for (entry in expression.entries) {
            entry.accept(visitor)
        }
        components.constantExpressionEvaluator.evaluateExpression(
            expression,
            context.trace,
            contextWithExpectedType.expectedType
        )
        return components.dataFlowAnalyzer.checkType(
            visitor.typeInfo.replaceType(components.builtIns.stringType),
            expression,

            contextWithExpectedType
        )
    }

    /**
     * 访问常量表达式
     *
     * 处理所有类型的字面量常量，包括：
     * - 整数字面量: `42`, `0xFF`, `0b1010`
     * - 浮点数字面量: `3.14`, `1.0e10`
     * - 布尔字面量: `true`, `false`
     * - 字符字面量: `'a'`, `'\n'`
     *
     * 该方法会：
     * 1. 检查字面量的前缀和后缀
     * 2. 检查数字字面量中下划线的使用
     * 3. 进行编译时常量求值
     * 4. 处理类型推导（例如根据预期类型选择 Int8/Int16/Int32/Int64）
     *
     * @param expression 常量表达式
     * @param context 表达式类型检查上下文
     * @return 常量的类型信息
     */
    //    根据字面量返回类型信息
    override fun visitConstantExpression(
        expression: CjConstantExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val elementType = expression.node.elementType
        if (elementType === CjNodeTypes.RUNE_CONSTANT || elementType === INTEGER_CONSTANT || elementType === CjNodeTypes.FLOAT_CONSTANT) {
            checkLiteralPrefixAndSuffix(expression, context)
        }


        if (elementType === INTEGER_CONSTANT || elementType === CjNodeTypes.FLOAT_CONSTANT) {
            checkUnderscores(expression, elementType, context)
        }

        val compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )

        if (compileTimeConstant is UnsignedErrorValueTypeConstant) {
            val value = compileTimeConstant.errorValue
            context.trace.report(UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH.on(expression))

            return createTypeInfo(value.getType(components.moduleDescriptor), context)
        } else if (compileTimeConstant !is IntegerValueTypeConstant && compileTimeConstant !is FloatValueTypeConstant) {
            val constantChecker = CompileTimeConstantChecker(context, components.moduleDescriptor, false)
            val constantValue =
                if (compileTimeConstant != null) (compileTimeConstant as TypedCompileTimeConstant<*>).constantValue else null
            val hasError = constantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType)
            if (hasError) {
                return createTypeInfo(
                    constantValue?.getType(components.moduleDescriptor) ?: getDefaultType(elementType),
                    context
                )
            }
        }

        checkNotNull(compileTimeConstant) {
            "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " +
                    expression.text
        }
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context)
    }

    override fun visitCollectionLiteralExpression(
        expression: CjCollectionLiteralExpression, context: ExpressionTypingContext
    ): CangJieTypeInfo {
        return components.collectionLiteralResolver.resolveCollectionLiteral(expression, context)
    }

    /**
     * 访问限定表达式（按枚举 case）
     *
     * 处理枚举类型的 case 构造调用，如 `MyEnum.Case(args)`。
     * 这是枚举类型的特殊处理路径。
     *
     * @param expression 限定表达式
     * @param argument 参数列表
     * @param context 表达式类型检查上下文
     * @return 枚举 case 实例的类型信息
     */
    fun visitQualifiedExpressionByCaseEnum(
        expression: CjQualifiedExpression,

        argument: List<ValueArgument>,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfoByCaseEnum(expression, argument, context)
    }

    fun visitQualifiedExpressionByEnum(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfoByEnum(expression, context)
    }

    /**
     * 访问限定表达式
     *
     * 处理成员访问表达式，如 `obj.property`, `obj.method()`, `Package.Class` 等。
     * 该方法会解析接收者类型，查找成员，处理扩展函数等。
     *
     * @param expression 限定表达式
     * @param context 表达式类型检查上下文
     * @return 成员的类型信息
     */
    override fun visitQualifiedExpression(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val callExpressionResolver = components.callExpressionResolver
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context)
    }

    override fun visitVariable(variable: CjVariable, context: ExpressionTypingContext): CangJieTypeInfo {
        components.localVariableResolver.process(variable, context, context.scope, facade)
        return declarationInIllegalContext(variable, context)
    }

    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val innerExpression = expression.expression ?: return noTypeInfo(context)
        var result = facade.getTypeInfo(innerExpression, context)
        val resultType = result.type
        if (resultType != null) {
            val innerValue = components.dataFlowValueFactory.createDataFlowValue(innerExpression, resultType, context)
            val resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, resultType, context)
            result = result.replaceDataFlowInfo(
                result.dataFlowInfo.assign(
                    resultValue, innerValue /*,             components.languageVersionSettings*/
                )
            )
        }
        return result
    }

    fun visitSimpleNameExpressionByCaseEnum(
        expression: CjSimpleNameExpression,
        argument: List<ValueArgument>,
        context: ExpressionTypingContext,
        isReportError: Boolean = true
    ): CangJieTypeInfo {

        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfoByCaseEnum(
            expression,
            null,
            null,
            context,
            argument,
            isReportError
        )


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }

    fun visitSimpleNameExpressionByEnum(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            ReservedCheckingCj.checkReservedYield(expression, context.trace);
//        }
//
//        // TODO : other members
//        // TODO : type substitutions???
        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfoByEnum(expression, null, null, context)


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }

    /**
     * 访问简单名称表达式
     *
     * 处理标识符引用，如变量名、函数名、类名等。该方法会：
     * 1. 在当前作用域中查找标识符
     * 2. 解析可能的重载
     * 3. 处理隐式接收者
     * 4. 进行编译时常量求值（如果适用）
     *
     * 特殊处理：
     * - 如果名称为 "VArray"，会使用特殊的 VArray 解析器
     *
     * @param expression 简单名称表达式
     * @param context 表达式类型检查上下文
     * @return 引用目标的类型信息
     */
    override fun visitSimpleNameExpression(
        expression: CjSimpleNameExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        if (expression.referencedName == "VArray") {
            return facade.components.vArrayResolver.resolve(expression, context)
        }

//        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
//            ReservedCheckingCj.checkReservedYield(expression, context.trace);
//        }
//
//        // TODO : other members
//        // TODO : type substitutions???
        val callExpressionResolver = components.callExpressionResolver
        val typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, null, null, context)


        checkNull(expression, context, typeInfo.type)

        components.constantExpressionEvaluator.evaluateExpression(
            expression, context.trace, context.expectedType
        )
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context) // TODO : Extensions to this
    }

    /**
     * 访问一元表达式
     *
     * 处理所有一元运算符表达式，包括：
     * - 前缀运算符: `++x`, `--x`, `+x`, `-x`, `!x`
     * - 后缀运算符: `x++`, `x--`
     *
     * 一元运算符会被解析为方法调用（运算符重载），例如：
     * - `++x` → `x.inc()` 然后赋值回 x
     * - `x++` → `x.inc()` 但返回原值
     * - `!x` → `x.not()`
     *
     * @param expression 一元表达式
     * @param contextWithExpectedType 包含预期类型的表达式类型检查上下文
     * @return 一元表达式的结果类型信息
     */
    override fun visitUnaryExpression(
        expression: CjUnaryExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType.replaceContextDependency(ContextDependency.INDEPENDENT)
            .replaceExpectedType(NO_EXPECTED_TYPE)

        val baseExpression = expression.baseExpression ?: return noTypeInfo(context)

        val operationSign = expression.operationReference

        val operationType = operationSign.referencedNameElementType


        // Type check the base expression
        var typeInfo = facade.safeGetTypeInfo(baseExpression, context)
        val type = ExpressionTypingUtils.safeGetType(typeInfo)
        val receiver = create(baseExpression, type, context.trace.bindingContext)

        val call = makeCall(receiver, expression)

        // Conventions for unary operations
        val name = OperatorConventions.UNARY_OPERATION_NAMES[operationType]
        if (name == null) {
            context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"))
            return typeInfo.clearType()
        }

        val deparenthesizedBaseExpression = CjPsiUtil.deparenthesize(baseExpression)


        // Resolve the operation reference
        val resolutionResults = components.callResolver.resolveCallWithGivenName(
            context, call, expression.operationReference, name
        )

        if (!resolutionResults.isSuccess) {
            return typeInfo.clearType()
        }

        // Computing the return type
        val returnType = resolutionResults.resultingDescriptor.returnType
        val result: CangJieType?
        if (operationType === CjTokens.PLUSPLUS || operationType === CjTokens.MINUSMINUS) {
            checkNotNull(returnType) { "returnType is null for " + resolutionResults.resultingDescriptor }
            if (isUnit(returnType)) {
                result = createErrorType(ErrorTypeKind.UNIT_RETURN_TYPE_FOR_INC_DEC)
                context.trace.report(INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign))
            } else {
                val receiverType = receiver.type
                if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(
                        RESULT_TYPE_MISMATCH.on(
                            operationSign,
                            name.asString(),
                            receiverType,
                            returnType
                        )
                    )
                } else {
                    context.trace.record(VARIABLE_REASSIGNMENT, expression)
                    val stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
                        baseExpression.project, context.trace, "e", type
                    )
                    checkLValue(context.trace, context, baseExpression, stubExpression, expression, false)
                }
                // x++ type is x type, but ++x type is x.inc() type
                val receiverValue = components.dataFlowValueFactory.createDataFlowValue(
                    (call.explicitReceiver as ReceiverValue?)!!, contextWithExpectedType
                )
                if (expression is CjPrefixExpression) {
                    result = returnType
                } else {
                    result = receiverType
                    // Also record data flow information for x++ value (= x)
                    val returnValue = components.dataFlowValueFactory.createDataFlowValue(
                        expression,
                        receiverType,
                        contextWithExpectedType
                    )
                    typeInfo = typeInfo.replaceDataFlowInfo(
                        typeInfo.dataFlowInfo.assign(
                            returnValue, receiverValue /*      ,
                            components.languageVersionSettings*/
                        )
                    )
                }
            }
        } else {
            result = returnType
        }

        val value = components.constantExpressionEvaluator.evaluateExpression(
            expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        )
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(
                value,
                expression,
                contextWithExpectedType
            )
        }

        return components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(result),
            expression,
            contextWithExpectedType.replaceDataFlowInfo(typeInfo.dataFlowInfo)
        )
    }

    /**
     * 检查给定的表达式是否可以作为赋值操作的左值（LValue）。
     *
     * @param trace 绑定跟踪对象，用于记录类型检查过程中的绑定信息。
     * @param context 表达式类型检查的上下文。
     * @param expressionWithParenthesis 带有括号的表达式。
     * @param rightHandSide 赋值操作的右值表达式。
     * @param operationExpression 操作表达式，通常是一个赋值或复合赋值操作。
     * @param arraySetMethodAlreadyResolved 数组设置方法是否已解析。
     * @return 如果表达式可以被赋值，则返回 `true`；否则返回 `false`。
     */
    fun checkLValue(
        trace: BindingTrace,
        context: ExpressionTypingContext,
        expressionWithParenthesis: CjExpression,
        rightHandSide: CjExpression?,
        operationExpression: CjOperationExpression,
        arraySetMethodAlreadyResolved: Boolean
    ): Boolean {
        // 去除表达式的括号
        val expression = CjPsiUtil.deparenthesize(expressionWithParenthesis)

        if (expression is CjArrayAccessExpression) {
            val arrayExpression = expression.arrayExpression ?: return false
            if (rightHandSide == null) return false

            val traceWithIndexedLValue: BindingTrace
            val methodSetIsResolved: Boolean

            if (!arraySetMethodAlreadyResolved) {
                val ignoreReportsTrace = TemporaryBindingTrace.create(trace, "Trace for checking set function")
                val findSetterContext = context.replaceBindingTrace(ignoreReportsTrace)
                val info = resolveArrayAccessSetMethod(expression, rightHandSide, findSetterContext, ignoreReportsTrace)

                traceWithIndexedLValue = ignoreReportsTrace
                methodSetIsResolved = info.type != null
            } else {
                traceWithIndexedLValue = trace
                methodSetIsResolved = true
            }

            val operationType = operationExpression.operationReference.referencedNameElementType
            if (operationType in CjTokens.AUGMENTED_ASSIGNMENTS ||
                operationType == CjTokens.PLUSPLUS ||
                operationType == CjTokens.MINUSMINUS
            ) {
                val resolvedCall = traceWithIndexedLValue[INDEXED_LVALUE_SET, expression]
                if (resolvedCall != null && trace.wantsDiagnostics()) {
                    val callCheckerContext = CallCheckerContext(
                        context,
                        components.deprecationResolver,
                        components.moduleDescriptor,
                        components.missingSupertypesResolver,
                        components.callComponents,
                        trace
                    )
                    components.callCheckers.forEach { it.check(resolvedCall, expression, callCheckerContext) }

                    // Ensure the resolved call for 'set' operator is recorded (see KT-36956)
                    if (trace[INDEXED_LVALUE_SET, expression] == null) {
                        trace.record(INDEXED_LVALUE_SET, expression, resolvedCall)
                    }
                }
            }

            return methodSetIsResolved
        }

        // 从绑定上下文中提取变量描述符
        val variable = BindingContextUtils.extractVariableDescriptorFromReference(trace.bindingContext, expression)

        var result = true
        var reportOn = expression ?: expressionWithParenthesis

        // 如果报告对象是限定表达式，则使用其选择器表达式
        if (reportOn is CjQualifiedExpression) {
            val selector = reportOn.selectorExpression
            if (selector != null) reportOn = selector
        }

        // 如果变量描述符为空，报告错误并设置结果为 false
        if (variable == null) {
            trace.report(VARIABLE_EXPECTED.on(reportOn))
            result = false
        } else if (!variable.isVar) {
            // 如果变量不是可变的（即不是 `var` 类型），设置结果为 false
            result = false
        }

        return result
    }


    private fun recordThisOrSuperCallInTraceAndCallExtension(
        context: ExpressionTypingContext,
        descriptor: ReceiverParameterDescriptor,
        expression: CjExpression
    ) {
        val trace = context.trace
        val call = makeCall(expression, null, null, expression, emptyList())
        val resolutionCandidate =
            OldResolutionCandidate.create(
                call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null
            )

        val resolvedCall =
            ResolvedCallImpl.create(
                resolutionCandidate,
                TemporaryBindingTrace.create(trace, "Fake trace for fake 'this' or 'super' resolved call"),
                TracingStrategy.EMPTY,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            )
        resolvedCall.markCallAsCompleted()

        trace.record(RESOLVED_CALL, call, resolvedCall)
        trace.record(CALL, expression, call)

        if (context.trace.wantsDiagnostics()) {
            val callCheckerContext =
                createCallCheckerContext(context)
            for (checker in components.callCheckers) {
                checker.check(resolvedCall, expression, callCheckerContext)
            }
        }
    }

    private fun createCallCheckerContext(context: ExpressionTypingContext): CallCheckerContext {
        return CallCheckerContext(
            context,
            components.deprecationResolver,
            components.moduleDescriptor,
            components.missingSupertypesResolver,
            components.callComponents
        )
    }

    // No class receivers
    private fun resolveToReceiver(
        expression: CjInstanceExpressionWithLabel,
        context: ExpressionTypingContext,
        onlyClassReceivers: Boolean
    ): LabelResolver.LabeledReceiverResolutionResult {
        val labelName = expression.getLabelNameAsName()
        if (labelName != null) {
            val resolutionResult =
                resolveThisOrSuperLabel(expression, context, labelName)
            if (resolutionResult.success()) {
                val receiverParameterDescriptor = resolutionResult.getReceiverParameterDescriptor()
                recordThisOrSuperCallInTraceAndCallExtension(context, receiverParameterDescriptor!!, expression)
                if (onlyClassReceivers && !isDeclaredInClass(
                        receiverParameterDescriptor
                    )
                ) {
                    return labelResolutionSuccess(null)
                }
            }
            return resolutionResult
        } else {
            var result: ReceiverParameterDescriptor? = null
            val receivers = context.scope.getImplicitReceiversHierarchy()
            if (onlyClassReceivers) {
                for (receiver in receivers) {
                    if (isDeclaredInClass(receiver)) {
                        result = receiver
                        break
                    }
                }
            } else if (receivers.isNotEmpty()) {
                // `this` cannot point to context receiver
                for (receiver in receivers) {
                    if (receiver.value !is ContextReceiver) {
                        result = receiver
                        break
                    }
                }
            }
            if (result != null) {
                context.trace.record(
                    REFERENCE_TARGET,
                    expression.instanceReference,
                    result.containingDeclaration
                )
                recordThisOrSuperCallInTraceAndCallExtension(context, result, expression)
            }
            return labelResolutionSuccess(result)
        }
    }

    private fun checkPossiblyQualifiedSuper(
        expression: CjSuperExpression,
        context: ExpressionTypingContext,
        thisReceiver: ReceiverParameterDescriptor
    ): CangJieType? {
        var result: CangJieType? = null
        val thisType = thisReceiver.type
        val supertypes = thisType.constructor.supertypes
        val substitutor = create(thisType)

        val superTypeQualifier = expression.superTypeQualifier
        if (superTypeQualifier != null) {
            val typeElement = superTypeQualifier.typeElement

            var classifierCandidate: DeclarationDescriptor? = null
            var supertype: CangJieType? = null
            var redundantTypeArguments: PsiElement? = null
            if (typeElement is CjUserType) {
                // This may be just a superclass name even if the superclass is generic
                if (typeElement.typeArguments.isEmpty()) {
                    classifierCandidate = components.typeResolver.resolveClass(
                        context.scope,
                        typeElement,
                        context.trace,
                        context.isDebuggerContext
                    )
                } else {
                    supertype =
                        components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
                    redundantTypeArguments = typeElement.typeArgumentList
                }
            } else {
                supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
            }

            if (classifierCandidate is TypeAliasDescriptor) {
                classifierCandidate = classifierCandidate.classDescriptor
            }

            if (supertype != null) {
                if (supertypes.contains(supertype)) {
                    result = supertype
                }
            } else if (classifierCandidate is ClassDescriptor) {
                for (declaredSupertype in supertypes) {
                    if (declaredSupertype.constructor == classifierCandidate.typeConstructor) {
                        result = substitutor.safeSubstitute(declaredSupertype, Variance.INVARIANT)
                        break
                    }
                }
            }

            val validClassifier = classifierCandidate != null && !isError(classifierCandidate)
            val validType = supertype != null && !supertype.isError
            if (result == null && (validClassifier || validType)) {
                context.trace.report(NOT_A_SUPERTYPE.on(superTypeQualifier))
            } else if (redundantTypeArguments != null) {
                context.trace.report(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.on(redundantTypeArguments))
            }


        } else {
            if (isPossiblyAmbiguousUnqualifiedSuper(expression, supertypes)) {
                val supertypesResolvedFromContextWithEqualsMigration =
                    resolveUnqualifiedSuperFromExpressionContext(
                        expression, supertypes, components.builtIns.anyType
                    )
                val supertypesResolvedFromContext = supertypesResolvedFromContextWithEqualsMigration.first
                if (supertypesResolvedFromContextWithEqualsMigration.second) {
                    context.trace.record(SUPER_EXPRESSION_FROM_ANY_MIGRATION, expression, true)
                }
                if (supertypesResolvedFromContext.size == 1) {
                    val singleResolvedType = supertypesResolvedFromContext.iterator().next()
                    result = substitutor.substitute(singleResolvedType, Variance.INVARIANT)
                } else if (supertypesResolvedFromContext.isEmpty()) {
                    // No supertype found, either with concrete or abstract members.
                    // Resolve to 'Any' (this will cause diagnostics for unresolved member reference).
                    result = components.builtIns.anyType
                } else {
                    context.trace.report(AMBIGUOUS_SUPER.on(expression))
                }
            } else {
                // supertypes may be empty when all the supertypes are error types (are not resolved, for example)
                val type = if (supertypes.isEmpty())
                    components.builtIns.anyType
                else
                    supertypes.iterator().next()
                result = substitutor.substitute(type, Variance.INVARIANT)
            }
        }
        if (result != null) {
            if (isInterface(thisType.constructor.declarationDescriptor)) {
                if (isClass(result.constructor.declarationDescriptor)) {
                    context.trace.report(SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.on(expression))
                }
            }
            context.trace.recordType(expression.instanceReference, result)
            result.constructor.declarationDescriptor?.let{
                context.trace.record(
                    REFERENCE_TARGET, expression.instanceReference,
                    it
                )
            }

            context.trace.record(THIS_TYPE_FOR_SUPER_EXPRESSION, expression, thisType)
        }

        context.trace.recordScope(context.scope, superTypeQualifier)
        return result
    }

    override fun visitSynchronizedExpression(
        expression: CjSynchronizedExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo {

        val blockExpression = expression.blockExpression ?: return noTypeInfo(data.dataFlowInfo)


        val newContext = data.replaceExpectedType(facade.components.builtIns.reentrantMutexType)

        expression.expression?.let { facade.getTypeInfo(it, newContext) }

        return facade.getTypeInfo(blockExpression, data)


    }

    /**
     * 访问 `this` 表达式
     *
     * 处理 `this` 关键字，用于引用当前接收者。可以是：
     * - 简单的 `this`：引用最近的接收者
     * - 带标签的 `this@Label`：引用特定标签的接收者
     *
     * 该方法会：
     * 1. 解析 this 引用的接收者
     * 2. 验证接收者是否存在
     * 3. 记录引用目标
     *
     * @param expression this 表达式
     * @param context 表达式类型检查上下文
     * @return this 接收者的类型信息
     */
    override fun visitThisExpression(expression: CjThisExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        var result: CangJieType? = null
        val resolutionResult = resolveToReceiver(expression, context, false)

        when (resolutionResult.code) {
            LabelResolver.LabeledReceiverResolutionResult.Code.LABEL_RESOLUTION_ERROR -> {}
            LabelResolver.LabeledReceiverResolutionResult.Code.NO_THIS -> context.trace.report(
                NO_THIS.on(
                    expression
                )
            )

            LabelResolver.LabeledReceiverResolutionResult.Code.SUCCESS -> {
                val descriptor = resolutionResult.getReceiverParameterDescriptor()
                descriptor?.let{
                    context.trace.record(THIS_REFERENCE_TARGET, expression.instanceReference, descriptor)

                }
                result = descriptor!!.type
                context.trace.recordType(expression.instanceReference, result)
            }
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression)
    }

    /**
     * 访问 `super` 表达式
     *
     * 处理 `super` 关键字，用于访问父类成员。可以是：
     * - 简单的 `super`：引用直接父类
     * - 限定的 `super<ParentClass>`：引用特定父类（用于多继承）
     * - 带标签的 `super@Label`：引用特定标签的父类
     *
     * 该方法会：
     * 1. 验证 super 只能在成员访问中使用（如 `super.method()`）
     * 2. 解析 super 引用的父类类型
     * 3. 处理可能的歧义（多个父类时）
     * 4. 记录引用目标
     *
     * @param expression super 表达式
     * @param context 表达式类型检查上下文
     * @return super 父类的类型信息
     */
    override fun visitSuperExpression(
        expression: CjSuperExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val resolutionResult = resolveToReceiver(expression, context, true)

        if (!CjPsiUtil.isLHSOfDot(expression)) {
            context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(expression, expression.text))
            return errorInSuper(expression, context)
        }

        when (resolutionResult.code) {
            LabelResolver.LabeledReceiverResolutionResult.Code.LABEL_RESOLUTION_ERROR ->                 // The error is already reported
                return errorInSuper(expression, context)

            LabelResolver.LabeledReceiverResolutionResult.Code.NO_THIS -> {
                context.trace.report(SUPER_NOT_AVAILABLE.on(expression))
                return errorInSuper(expression, context)
            }

            LabelResolver.LabeledReceiverResolutionResult.Code.SUCCESS -> {
                val result = checkPossiblyQualifiedSuper(
                    expression, context,
                    resolutionResult.getReceiverParameterDescriptor()!!
                )
                if (result != null) {
                    context.trace.recordType(expression.instanceReference, result)
                }
                return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression)
            }
        }
        throw IllegalStateException("Unknown code: " + resolutionResult.code)
    }

    private fun errorInSuper(expression: CjSuperExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        val superTypeQualifier = expression.superTypeQualifier
        if (superTypeQualifier != null) {
            components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true)
        }
        return noTypeInfo(context)
    }

    override fun visitDeclaration(dcl: CjDeclaration, context: ExpressionTypingContext): CangJieTypeInfo {
        return declarationInIllegalContext(dcl, context)
    }

    /**
     * 伴生对象
     *
     * 包含用于表达式类型检查的静态工具方法和常量。
     */
    companion object {

        /**
         * 允许使用裸类型的操作符集合
         *
         * 某些操作符（如 `as` 类型转换）允许在类型参数中使用裸类型。
         */
        val BARE_TYPES_ALLOWED: TokenSet = TokenSet.create(
            AS_KEYWORD,

            )

        /**
         * 判断表达式是否为左值
         *
         * 检查简单名称表达式是否出现在赋值操作的左侧，即是否作为左值使用。
         *
         * @param expression 简单名称表达式
         * @param parent 父 PSI 元素
         * @return 如果是左值返回 true，否则返回 false
         */
        fun isLValue(expression: CjSimpleNameExpression, parent: PsiElement?): Boolean {
            if (parent !is CjBinaryExpression) {
                return false
            }

            //        if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
//                !CjTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
//            return false;
//        }
            return PsiTreeUtil.isAncestor(parent.left, expression, false)
        }

        private fun isLValueOrUnsafeReceiver(expression: CjSimpleNameExpression): Boolean {
            val parent = PsiTreeUtil.skipParentsOfType(
                expression,
                CjParenthesizedExpression::class.java
            )
            if (parent is CjQualifiedExpression) {
                // so we have to analyze its nullability here

                return parent.operationSign === CjTokens.DOT &&
                        parent.receiverExpression === CjPsiUtil.deparenthesize(expression)
            }

            return isLValue(expression, parent)
        }

        private fun getTypeInfo(
            expression: CjExpression,
            facade: ExpressionTypingInternals,
            context: ExpressionTypingContext,
            forceExpressionResolve: Boolean
        ): CangJieTypeInfo? {
            return if (forceExpressionResolve) {
                facade.getTypeInfo(expression, context)
            } else {
                BindingContextUtils.getRecordedTypeInfo(expression, context.trace.bindingContext)
            }
        }

        private fun computeAccumulatedInfoForArrayAccessExpression(
            arrayTypeInfo: CangJieTypeInfo,
            indices: List<CjExpression>,
            rightHandSide: CjExpression?,
            isGet: Boolean,
            context: ExpressionTypingContext,
            facade: ExpressionTypingInternals
        ): CangJieTypeInfo {
            var accumulatedTypeInfo: CangJieTypeInfo? = null
            // 默认使用改进的类型推断系统
            val forceResolve = false

            // The accumulated data flow info of all index expressions is saved on the last index
            if (indices.isNotEmpty()) {
                accumulatedTypeInfo = getTypeInfo(indices[indices.size - 1], facade, context, forceResolve)
            }

            if (!isGet && rightHandSide != null) {
                accumulatedTypeInfo = getTypeInfo(rightHandSide, facade, context, forceResolve)
            }

            return accumulatedTypeInfo ?: arrayTypeInfo
        }

        //字面量前缀和后缀
        private fun checkLiteralPrefixAndSuffix(expression: PsiElement, context: ExpressionTypingContext) {
            if (expression is StubBasedPsiElement<*> && expression.stub != null) {
                return
            }

            checkLiteralPrefixOrSuffix(PsiTreeUtil.prevLeaf(expression), context)
            checkLiteralPrefixOrSuffix(PsiTreeUtil.nextLeaf(expression), context)
        }

        //    前缀或后缀
        private fun checkLiteralPrefixOrSuffix(prefixOrSuffix: PsiElement?, context: ExpressionTypingContext) {
            if (illegalLiteralPrefixOrSuffix(prefixOrSuffix)) {
                prefixOrSuffix?.let { context.trace.report(UNSUPPORTED.on(it, "literal prefixes and suffixes")) }
            }
        }

        private fun illegalLiteralPrefixOrSuffix(element: PsiElement?): Boolean {
            if (element == null) return false

            val elementType = element.node.elementType
            return elementType === CjTokens.IDENTIFIER || elementType === CjTokens.INTEGER_LITERAL || elementType === CjTokens.FLOAT_LITERAL ||
                    elementType is CjKeywordToken
        }

        private fun declarationInIllegalContext(
            declaration: CjDeclaration,
            context: ExpressionTypingContext
        ): CangJieTypeInfo {
            context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(declaration))
            return noTypeInfo(context)
        }

        private fun isDeclaredInClass(receiver: ReceiverParameterDescriptor): Boolean {
            return receiver.containingDeclaration is ClassDescriptor
        }

        private fun checkResolvedExplicitlyQualifiedSupertype(
            trace: BindingTrace,
            result: CangJieType,
            supertypes: Collection<CangJieType>,
            superTypeQualifier: CjTypeReference
        ) {
            if (supertypes.size > 1) {
                val resultClassifierDescriptor = result.constructor.declarationDescriptor
                for (otherSupertype in supertypes) {
                    val otherSupertypeClassifierDescriptor = otherSupertype.constructor.declarationDescriptor
                    if (otherSupertypeClassifierDescriptor === resultClassifierDescriptor) {
                        continue
                    }
                    if (CangJieTypeChecker.DEFAULT.isSubtypeOf(otherSupertype, result)) {
                        trace.report(
                            QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE.on(
                                superTypeQualifier,
                                otherSupertype
                            )
                        )
                        break
                    }
                }
            }
        }
    }
}
