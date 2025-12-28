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
 */

package org.cangnova.cangjie.types.expressions.match

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.ITERABLE
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.SENSELESS_NULL_IN_MATCH
import org.cangnova.cangjie.diagnostics.infos.warnings.USELESS_IS_CHECK
import org.cangnova.cangjie.diagnostics.infos.warnings.USELESS_NULLABLE_CHECK
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.elementType
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.BOOLEAN_CONSTANT
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.UNIT_CONSTANT
import org.cangnova.cangjie.resolve.DescriptorResolver.Companion.getDefaultVisibility
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import org.cangnova.cangjie.resolve.TypeResolutionContext
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DATAFLOW_INFO_AFTER_CONDITION
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IMPLICIT_EXHAUSTIVE_WHEN
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.SMARTCAST
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.TYPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.VARIABLE
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.caches.ConfusingMatchBranchSyntaxChecker
import org.cangnova.cangjie.resolve.caches.PrimitiveNumericComparisonCallChecker
import org.cangnova.cangjie.resolve.calls.checkers.RttiExpressionInformation
import org.cangnova.cangjie.resolve.calls.checkers.RttiOperation
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import org.cangnova.cangjie.types.checker.TypeIntersector
import org.cangnova.cangjie.types.expressions.*
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfMatchCall
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import java.util.*

/**
 * match 表达式分析结果
 *
 * @property patterns 解析得到的模式列表
 * @property exhaustivenessResult 穷举性检查结果
 * @property bindings 所有模式中的变量绑定
 */
data class MatchAnalysisResult(
    val patterns: List<Pattern>,
    val exhaustivenessResult: ExhaustivenessResult,
    val bindings: List<List<PatternBinding>>
)

/**
 * 模式分析器（PatternAnalyzer）
 *
 * 模式匹配分析的统一入口类。协调模式解析、变量绑定收集、
 * 穷举性检查、类型检查等各个组件的工作。
 *
 * ## 核心职责
 *
 * 1. **协调分析流程**: 管理模式分析的完整流程
 * 2. **统一入口**: 为不同场景提供统一的 API
 * 3. **整合结果**: 收集各组件的分析结果
 *
 * ## 支持的场景
 *
 * - **match 表达式**: 完整的模式匹配分析
 * - **let 声明**: 变量声明中的解构模式
 * - **for-in 循环**: 迭代变量的解构模式
 * - **let 表达式**: 条件绑定表达式
 * - **is 表达式**: 类型检查表达式
 */
class PatternAnalyzer(
    private val components: ExpressionTypingComponents,
    private val facade: ExpressionTypingInternals
) {

    private val resolver = PatternResolver(components, facade)
    private val bindingCollector = BindingCollector(components)
 
    // ===== match 表达式分析 =====

    /**
     * 分析 match 表达式
     *
     * @param expression match 表达式
     * @param contextWithExpectedType 带有期望类型的上下文
     * @return 类型信息
     */
    fun analyzeMatchExpression(
        expression: CjMatchExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val trace = contextWithExpectedType.trace
        checkDeprecatedMatchSyntax(trace, expression)

        components.dataFlowAnalyzer.recordExpectedType(trace, expression, contextWithExpectedType.expectedType)
        val contextBeforeSubject = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        // 创建 Subject
        val subject = createSubjectFromExpression(expression.subjectExpression, contextBeforeSubject)

        // 创建 match 作用域
        val matchScope = ExpressionTypingUtils.newWritableScopeImpl(
            contextWithExpectedType,
            LexicalScopeKind.MATCH,
            components.overloadChecker
        )
        val matchContext = contextWithExpectedType.replaceScope(matchScope)

        subject.initDataFlowValue(matchContext, components.builtIns)

        // 分析所有条件
        val dataFlowInfoForEntries = analyzeConditionsInMatchEntries(expression, matchContext, subject)

        // 推断返回类型
        val matchReturnType = inferTypeForMatchExpression(
            expression, subject, matchContext, matchContext, dataFlowInfoForEntries
        )

        val matchResultValue = matchReturnType?.let {
            components.dataFlowValueFactory.createDataFlowValue(expression, it, matchContext)
        }

        // 合并分支类型信息
        val branchesTypeInfo = joinMatchExpressionBranches(
            expression, matchContext, matchReturnType, subject.jumpOutPossible, matchResultValue
        )

        // 穷举性检查
        val isExhaustive = checkMatchExhaustive(expression, trace)

        val branchesDataFlowInfo = branchesTypeInfo.dataFlowInfo
        val resultDataFlowInfo = if (expression.elseExpression == null && !isExhaustive) {
            branchesDataFlowInfo.or(matchContext.dataFlowInfo)
        } else {
            branchesDataFlowInfo
        }

        if (matchReturnType != null && isExhaustive && expression.elseExpression == null &&
            CangJieBuiltIns.isNothing(matchReturnType)
        ) {
            trace.record(IMPLICIT_EXHAUSTIVE_WHEN, expression)
        }

        val branchesType = branchesTypeInfo.type ?: return noTypeInfo(resultDataFlowInfo)
        val resultType = components.dataFlowAnalyzer.checkType(branchesType, expression, contextWithExpectedType)

        ConfusingMatchBranchSyntaxChecker.check(expression, contextWithExpectedType.languageVersionSettings, trace)

        return createTypeInfo(
            resultType,
            resultDataFlowInfo,
            branchesTypeInfo.jumpOutPossible,
            contextWithExpectedType.dataFlowInfo
        )
    }

    /**
     * 从表达式创建 Subject
     */
    private fun createSubjectFromExpression(
        subjectExpression: CjExpression?,
        context: ExpressionTypingContext
    ): Subject {
        return when {
            subjectExpression != null -> Subject.Expression(
                subjectExpression,
                facade.getTypeInfo(subjectExpression, context),
                components.dataFlowValueFactory
            )
            else -> Subject.None()
        }
    }

    /**
     * 分析 match 条目中的所有条件
     */
    private fun analyzeConditionsInMatchEntries(
        expression: CjMatchExpression,
        contextAfterSubject: ExpressionTypingContext,
        subject: Subject
    ): ArrayList<DataFlowInfo> {
        val argumentDataFlowInfos = ArrayList<DataFlowInfo>()
        var inputDataFlowInfo = contextAfterSubject.dataFlowInfo

        for (matchEntry in expression.entries) {
            val conditionsInfo = analyzeMatchEntryConditions(
                matchEntry,
                contextAfterSubject.replaceDataFlowInfo(inputDataFlowInfo),
                subject
            )
            inputDataFlowInfo = inputDataFlowInfo.and(conditionsInfo.elseInfo)

            if (matchEntry.expression != null) {
                argumentDataFlowInfos.add(conditionsInfo.thenInfo)
            }

            // 处理模式守卫
            matchEntry.patternGuard?.expression?.let {
                facade.getTypeInfo(it, contextAfterSubject.replaceExpectedType(components.builtIns.boolType))
            }
        }

        return argumentDataFlowInfos
    }

    /**
     * 分析单个 match 条目的条件
     */
    private fun analyzeMatchEntryConditions(
        matchEntry: CjMatchEntry,
        context: ExpressionTypingContext,
        subject: Subject
    ): ConditionalDataFlowInfo {
        if (matchEntry.isElse) {
            return ConditionalDataFlowInfo(context.dataFlowInfo)
        }

        var entryInfo: ConditionalDataFlowInfo? = null

        val caseScope = ExpressionTypingUtils.newWritableScopeImpl(
            context, LexicalScopeKind.MATCH_CASE, components.overloadChecker
        )
        var contextForCondition = context.replaceScope(caseScope)

        for (condition in matchEntry.conditions) {
            val conditionInfo = resolvePatternCondition(subject, condition, contextForCondition)
            entryInfo = entryInfo?.let {
                ConditionalDataFlowInfo(
                    it.thenInfo.or(conditionInfo.thenInfo),
                    it.elseInfo.and(conditionInfo.elseInfo)
                )
            } ?: conditionInfo

            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfo.elseInfo)
        }
        context.trace.recordScope(caseScope, matchEntry.body)

        return entryInfo ?: ConditionalDataFlowInfo(context.dataFlowInfo)
    }

    /**
     * 解析模式条件
     */
    private fun resolvePatternCondition(
        subject: Subject,
        condition: CjCasePatternElement,
        context: ExpressionTypingContext
    ): ConditionalDataFlowInfo {
        val noChange = ConditionalDataFlowInfo(context.dataFlowInfo)

        return when (condition) {
            is CjMatchConditionWithExpression -> {
                val expression = condition.expression ?: return noChange
                val basicDataFlowInfo = checkTypeForExpressionCondition(context, expression, subject)
                val moduleDescriptor = DescriptorUtils.getContainingModule(context.scope.ownerDescriptor)
                val dataFlowInfoFromES = components.effectSystem.getDataFlowInfoMatchEquals(
                    subject.valueExpression, expression, context.trace, moduleDescriptor
                )
                basicDataFlowInfo.and(dataFlowInfoFromES)
            }
            else -> {
                val patternContext = ExtendedPatternContext(
                    subject = subject,
                    typingContext = context,
                    source = PatternSource.MATCH_EXPRESSION
                )
                val pattern = resolver.resolve(condition, patternContext)

                // 收集并声明模式中的变量绑定
                val scope = context.scope
                if (scope is LexicalWritableScope) {
                    bindingCollector.collectAndDeclare(
                        pattern = pattern,
                        context = patternContext,
                        scope = scope,
                        trace = context.trace,
                        localVariableResolver = components.localVariableResolver,
                        isLocal = true
                    )
                }
                noChange
            }
        }
    }

    /**
     * 推断 match 表达式的类型
     */
    private fun inferTypeForMatchExpression(
        expression: CjMatchExpression,
        subject: Subject,
        contextWithExpectedType: ExpressionTypingContext,
        contextAfterSubject: ExpressionTypingContext,
        dataFlowInfoForEntries: List<DataFlowInfo>
    ): CangJieType? {
        if (expression.entries.all { it.expression == null }) {
            return components.builtIns.unitType
        }

        val wrappedArgumentExpressions = wrapMatchEntryExpressionsAsSpecialCallArguments(expression)
        val callForMatch = createCallForSpecialConstruction(
            expression,
            subject.getCalleeExpressionForSpecialCall() ?: expression,
            wrappedArgumentExpressions
        )
        val dataFlowInfoForArguments = createDataFlowInfoForArgumentsOfMatchCall(
            callForMatch, contextAfterSubject.dataFlowInfo, dataFlowInfoForEntries
        )

        val resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
            callForMatch,
            ControlStructureTypingUtils.ResolveConstruct.MATCH,
            object : AbstractList<String>() {
                override fun get(index: Int): String = "constructor$index"
                override val size: Int get() = wrappedArgumentExpressions.size
            },
            Collections.nCopies(wrappedArgumentExpressions.size, false),
            contextWithExpectedType,
            dataFlowInfoForArguments
        )

        return resolvedCall.resultingDescriptor.returnType
    }

    private fun wrapMatchEntryExpressionsAsSpecialCallArguments(expression: CjMatchExpression): List<CjExpression> {
        val psiFactory = CjPsiFactory(expression.project)
        return expression.entries.mapNotNull { matchEntry ->
            matchEntry.expression?.let { psiFactory.wrapInABlockWrapper(it) }
        }
    }

    /**
     * 合并 match 分支的类型信息
     */
    private fun joinMatchExpressionBranches(
        expression: CjMatchExpression,
        contextAfterSubject: ExpressionTypingContext,
        resultType: CangJieType?,
        jumpOutPossibleInSubject: Boolean,
        whenResultValue: DataFlowValue?
    ): CangJieTypeInfo {
        val bindingContext = contextAfterSubject.trace.bindingContext

        var currentDataFlowInfo: DataFlowInfo? = null
        var jumpOutPossible = jumpOutPossibleInSubject
        var errorTypeExistInBranch = false

        for (whenEntry in expression.entries) {
            val entryExpression = whenEntry.expression ?: continue
            val entryTypeInfo = BindingContextUtils.getRecordedTypeInfo(entryExpression, bindingContext) ?: continue
            val entryType = entryTypeInfo.type

            if (entryType == null) {
                errorTypeExistInBranch = true
            }

            val entryDataFlowInfo = if (whenResultValue != null && entryType != null) {
                val entryValue = components.dataFlowValueFactory.createDataFlowValue(
                    entryExpression, entryType, contextAfterSubject
                )
                entryTypeInfo.dataFlowInfo.assign(whenResultValue, entryValue)
            } else {
                entryTypeInfo.dataFlowInfo
            }

            currentDataFlowInfo = when {
                entryType != null && CangJieBuiltIns.isNothing(entryType) -> currentDataFlowInfo
                currentDataFlowInfo != null -> currentDataFlowInfo.or(entryDataFlowInfo)
                else -> entryDataFlowInfo
            }

            jumpOutPossible = jumpOutPossible or entryTypeInfo.jumpOutPossible
        }

        val resultDataFlowInfo = currentDataFlowInfo ?: contextAfterSubject.dataFlowInfo
        return if (resultType == null || errorTypeExistInBranch && CangJieBuiltIns.isNothing(resultType))
            noTypeInfo(resultDataFlowInfo)
        else
            createTypeInfo(resultType, resultDataFlowInfo, jumpOutPossible, resultDataFlowInfo)
    }

    // ===== let 表达式分析 =====

    /**
     * 分析 let 表达式
     *
     * @param expression let 表达式
     * @param context 类型检查上下文
     * @return 类型信息
     */
    fun analyzeLetExpression(
        expression: CjLetExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        if (expression.pattern is CjTypePattern) {
            context.trace.report(LET_EXPRESSION_NO_TYPE_PATTERN.on(expression.pattern ?: return noTypeInfo(context)))
        }

        val subjectExpression = expression.expression
        val subject = createSubjectFromExpression(subjectExpression, context)

        val contextAfterSubject = run {
            var result = context
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }

        subject.initDataFlowValue(contextAfterSubject, components.builtIns)

        expression.pattern?.let {
            val patternContext = ExtendedPatternContext(
                subject = subject,
                typingContext = contextAfterSubject,
                source = PatternSource.LET_EXPRESSION
            )
            val pattern = resolver.resolve(it, patternContext)

            // 收集并声明模式中的变量绑定
            val scope = contextAfterSubject.scope
            if (scope is LexicalWritableScope) {
                bindingCollector.collectAndDeclare(
                    pattern = pattern,
                    context = patternContext,
                    scope = scope,
                    trace = contextAfterSubject.trace,
                    localVariableResolver = components.localVariableResolver,
                    isLocal = true
                )
            }
        }

        return noTypeInfo(context)
    }

    // ===== 模式变量声明分析 =====

    /**
     * 分析模式变量声明
     *
     * @param variable 模式变量
     * @param context 类型检查上下文
     * @return 类型信息
     */
    fun analyzePatternVariable(
        variable: CjPatternVariable,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val newContext = context.replaceContextDependency(ContextDependency.INDEPENDENT)
        val visibility = resolveVisibilityFromModifiers(
            variable,
            getDefaultVisibility(variable, context.scope.ownerDescriptor)
        )

        val pattern = variable.pattern
        val typeReference = variable.typeReference
        val expectedType = typeReference?.let {
            components.typeResolver.resolveType(newContext.scope, it, newContext.trace, true)
        }

        val initializer = variable.initializer
        val initializerTypeInfo = initializer?.let {
            facade.getTypeInfo(it, newContext.replaceExpectedType(expectedType))
        } ?: noTypeInfo(context)

        val subject = when {
            initializer != null -> Subject.Expression(
                initializer,
                expectedType?.let { createTypeInfo(it) } ?: initializerTypeInfo,
                components.dataFlowValueFactory
            )
            expectedType != null -> Subject.Type(createTypeInfo(expectedType), null, newContext)
            else -> Subject.None()
        }

        val contextAfterSubject = run {
            var result = newContext
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }

        subject.initDataFlowValue(contextAfterSubject, components.builtIns)

        pattern?.let {
            val patternContext = ExtendedPatternContext(
                subject = subject,
                typingContext = newContext,
                source = PatternSource.LET_DECLARATION,
                expectedType = subject.type
            )
            val config = PatternResolverConfig(
                isVar = variable.isVar,
                bindEnumEntry = false,
                isLocal = variable.isLocal,
                visibility = visibility
            )
            val resolvedPattern = resolver.resolve(it, patternContext, config)

            // 收集并声明模式中的变量绑定
            val scope = newContext.scope
            if (scope is LexicalWritableScope) {
                bindingCollector.collectAndDeclare(
                    pattern = resolvedPattern,
                    context = patternContext,
                    scope = scope,
                    trace = newContext.trace,
                    localVariableResolver = components.localVariableResolver,
                    isLocal = variable.isLocal
                )
            }

            // 检查可反驳性
            checkRefutabilityForVariableDeclaration(it, resolvedPattern, newContext.trace)
        }

        return createTypeInfo(components.builtIns.unitType)
    }

    // ===== for-in 循环模式分析 =====

    /**
     * 定义 for-in 循环中的模式变量
     *
     * @param writableScope 可写作用域
     * @param casePattern 模式元素
     * @param receiver 接收者值
     * @param initializer 初始化表达式
     * @param context 类型检查上下文
     */
    fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePatternElement,
        receiver: ReceiverValue,
        initializer: CjExpression?,
        context: ExpressionTypingContext
    ) {
        initializer ?: return
        if (receiver.type.isError) return

        val iterator = receiver.type.extractSuperType(ITERABLE)
        val iteratedType = iterator.arguments.first().type

        val subject = Subject.Expression(
            initializer,
            createTypeInfo(iteratedType),
            context.dataFlowValueFactory
        ).apply {
            initDataFlowValue(context, components.builtIns)
        }

        val patternContext = ExtendedPatternContext(
            subject = subject,
            typingContext = context.replaceScope(writableScope),
            source = PatternSource.FOR_IN_EXPRESSION,
            expectedType = iteratedType
        )
        val config = PatternResolverConfig(bindEnumEntry = false)
        val pattern = resolver.resolve(casePattern, patternContext, config)

        // 收集并声明模式中的变量绑定
        bindingCollector.collectAndDeclare(
            pattern = pattern,
            context = patternContext,
            scope = writableScope,
            trace = context.trace,
            localVariableResolver = components.localVariableResolver,
            isLocal = true
        )

        checkRefutabilityForForIn(casePattern, pattern, context.trace)
    }

    // ===== is 表达式分析 =====

    /**
     * 分析 is 表达式
     *
     * @param expression is 表达式
     * @param contextWithExpectedType 带有期望类型的上下文
     * @return 类型信息
     */
    fun analyzeIsExpression(
        expression: CjIsExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType
            .replaceExpectedType(NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        val leftHandSide = expression.leftHandSide
        val typeInfo = facade.safeGetTypeInfo(leftHandSide, context)
        val knownType = typeInfo.type
        val typeReference = expression.typeReference

        if (typeReference != null && knownType != null) {
            val dataFlowValue = components.dataFlowValueFactory.createDataFlowValue(leftHandSide, knownType, context)
            val conditionInfo = checkTypeForIs(context, expression, knownType, typeReference, dataFlowValue).thenInfo
            val newDataFlowInfo = conditionInfo.and(typeInfo.dataFlowInfo)
            context.trace.record(DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo)
        }

        val resultTypeInfo = components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(components.builtIns.boolType),
            expression,
            contextWithExpectedType
        )

        if (typeReference != null) {
            val rhsType = context.trace[TYPE, typeReference]
            val rttiInformation = RttiExpressionInformation(
                subject = leftHandSide,
                sourceType = knownType,
                targetType = rhsType,
                operation = RttiOperation.IS
            )
            components.rttiExpressionCheckers.forEach {
                it.check(rttiInformation, expression, context.trace)
            }
        }

        return resultTypeInfo
    }

    /**
     * 检查 is 表达式的类型
     */
    private fun checkTypeForIs(
        context: ExpressionTypingContext,
        isCheck: CjElement,
        subjectType: CangJieType,
        typeReferenceAfterIs: CjTypeReference,
        subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        val typeResolutionContext = TypeResolutionContext(
            context.scope, context.trace, true, true, context.isDebuggerContext
        )
        val possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs)
        val targetType = TypeReconstructionUtil.reconstructBareType(
            typeReferenceAfterIs, possiblyBareTarget, subjectType, context.trace, components.builtIns
        )

        if (targetType.isDynamic()) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs))
        }

        val targetDescriptor = TypeUtils.getClassDescriptor(targetType)
        if (targetDescriptor != null && DescriptorUtils.isEnumConstructor(targetDescriptor)) {
            context.trace.report(IS_ENUM_ENTRY.on(typeReferenceAfterIs))
        }

        if (!subjectType.containsError() && !TypeUtils.isOptionType(subjectType) && targetType.isOption) {
            val element = typeReferenceAfterIs.typeElement
            assert(element is CjOptionType) { "element must be instance of ${CjOptionType::class.java.name}" }
            context.trace.report(USELESS_NULLABLE_CHECK.on(element as CjOptionType))
        }

        val typesAreCompatible = checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs)
        detectRedundantIs(context, subjectType, targetType, isCheck, subjectDataFlowValue, typesAreCompatible)

        return context.dataFlowInfo.let {
            ConditionalDataFlowInfo(
                it.establishSubtyping(subjectDataFlowValue, targetType, components.languageVersionSettings),
                it
            )
        }
    }

    private fun detectRedundantIs(
        context: ExpressionTypingContext,
        subjectType: CangJieType,
        targetType: CangJieType,
        isCheck: CjElement,
        subjectDataFlowValue: DataFlowValue,
        typesAreCompatible: Boolean
    ) {
        if (subjectType.containsError() || targetType.containsError()) return

        val possibleTypes = DataFlowAnalyzer.getAllPossibleTypes(
            subjectType, context, subjectDataFlowValue, context.languageVersionSettings
        )

        if (!typesAreCompatible && !targetType.isError) {
            context.trace.report(USELESS_IS_CHECK.on(isCheck, false))
        } else if (CastDiagnosticsUtil.isRefinementUseless(possibleTypes, targetType, false)) {
            context.trace.report(USELESS_IS_CHECK.on(isCheck, true))
        }
    }

    // ===== 表达式条件检查 =====

    /**
     * 检查表达式条件的类型
     */
    private fun checkTypeForExpressionCondition(
        context: ExpressionTypingContext,
        expression: CjExpression,
        subject: Subject
    ): ConditionalDataFlowInfo {
        val noChange = ConditionalDataFlowInfo(context.dataFlowInfo)
        var newContext = context
        val typeInfo = facade.getTypeInfo(expression, newContext)
        val type = typeInfo.type ?: return noChange
        newContext = newContext.replaceDataFlowInfo(typeInfo.dataFlowInfo)

        if (subject is Subject.None) {
            val booleanType = components.builtIns.boolType
            val checkedTypeInfo = components.dataFlowAnalyzer.checkType(
                typeInfo, expression, newContext.replaceExpectedType(booleanType)
            )
            if (CangJieTypeChecker.DEFAULT.equalTypes(booleanType, checkedTypeInfo.type ?: type)) {
                val ifInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, true, newContext)
                val elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, false, newContext)
                return ConditionalDataFlowInfo(ifInfo, elseInfo)
            }
            return noChange
        }

        checkTypeCompatibility(newContext, type, subject.type, expression, true)
        val expressionDataFlowValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, newContext)

        val subjectStableTypes = listOf(subject.type) + context.dataFlowInfo.getStableTypes(
            subject.dataFlowValue, components.languageVersionSettings
        )
        val expressionStableTypes = listOf(type) + newContext.dataFlowInfo.getStableTypes(
            expressionDataFlowValue, components.languageVersionSettings
        )

        PrimitiveNumericComparisonCallChecker.inferPrimitiveNumericComparisonType(
            context.trace, subjectStableTypes, expressionStableTypes, expression
        )

        return ConditionalDataFlowInfo(
            noChange.thenInfo.equate(
                subject.dataFlowValue, expressionDataFlowValue,
                identityEquals = components.dataFlowAnalyzer.typeHasEqualsFromAny(subject.type, expression),
                languageVersionSettings = components.languageVersionSettings
            ),
            noChange.elseInfo.disequate(
                subject.dataFlowValue, expressionDataFlowValue, components.languageVersionSettings
            )
        )
    }

    // ===== 类型兼容性检查 =====

    /**
     * 检查类型兼容性
     */
    private fun checkTypeCompatibility(
        context: ExpressionTypingContext,
        type: CangJieType,
        subjectType: CangJieType,
        reportErrorOn: CjElement,
        isReportError: Boolean = false
    ): Boolean {
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
            if (isReportError) {
                context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType))
            }
            return false
        }

        checkEnumsForCompatibility(context, reportErrorOn, subjectType, type)

        if (CangJieBuiltIns.isNothing(type) && !TypeUtils.isOptionType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_MATCH.on(reportErrorOn))
        }
        return true
    }

    // ===== 穷举性检查 =====

    /**
     * 检查 match 表达式的穷举性
     */
    fun checkMatchExhaustive(expression: CjMatchExpression, trace: BindingTrace): Boolean {
        val result = ExhaustivenessAnalyzer.checkMatch(expression, trace.bindingContext)
        return result.isExhaustive
    }

    /**
     * 检查穷举性（返回详细结果）
     */
    fun checkExhaustiveness(
        expression: CjMatchExpression,
        context: BindingContext
    ): ExhaustivenessResult {
        return ExhaustivenessAnalyzer.checkMatch(expression, context)
    }

    // ===== 可反驳性检查 =====

    /**
     * for-in 循环的可反驳性检查
     *
     * for-in 循环只允许不可反驳的模式，因为每个迭代元素都必须匹配。
     *
     * @param psiPattern PSI 模式元素（用于报告错误位置）
     * @param resolvedPattern 解析后的模式对象
     * @param trace 绑定追踪器
     */
    private fun checkRefutabilityForForIn(
        psiPattern: CjCasePatternElement,
        resolvedPattern: Pattern,
        trace: BindingTrace
    ) {
        val result = RefutabilityChecker.check(resolvedPattern)
        // for-in 不允许可反驳的模式，也不允许类型模式（运行时检查）
        if (result.isRefutable || psiPattern is CjTypePattern) {
            trace.report(IRREFUTABLE_PATTERN_FOR_IN_ERROR.on(psiPattern))
        }
    }

    /**
     * 变量声明的可反驳性检查
     *
     * let 声明只允许不可反驳的模式，因为必须保证变量一定能被绑定。
     *
     * @param psiPattern PSI 模式元素（用于报告错误位置）
     * @param resolvedPattern 解析后的模式对象
     * @param trace 绑定追踪器
     */
    private fun checkRefutabilityForVariableDeclaration(
        psiPattern: CjCasePatternElement,
        resolvedPattern: Pattern,
        trace: BindingTrace
    ) {
        val result = RefutabilityChecker.check(resolvedPattern)
        // let 声明不允许可反驳的模式，也不允许类型模式（运行时检查）
        if (result.isRefutable || psiPattern is CjTypePattern) {
            trace.report(IRREFUTABLE_PATTERN_ERROR.on(psiPattern))
        }
    }

    // ===== 语法检查 =====

    /**
     * 检查没有条件的 match 表达式的语法
     */
    private fun checkDeprecatedMatchSyntax(trace: BindingTrace, expression: CjMatchExpression) {
        if (expression.subjectExpression != null) return

        for (entry in expression.entries) {
            if (entry.isElse) continue
            var child: PsiElement? = entry.firstChild
            while (child != null) {
                if (child.node.elementType === CjTokens.OR) {
                    trace.report(COMMA_IN_MATCH_CONDITION_WITHOUT_ARGUMENT.on(child))
                }
                if (child.node.elementType === CjTokens.DOUBLE_ARROW) break
                child = child.nextSibling
            }
        }
    }

    /**
     * 检查 | 连接符的变量引入冲突
     */
    fun checkConnector(matchExpression: CjMatchExpression, trace: BindingTrace) {
        val bindingContext = trace.bindingContext

        fun reportVariableIntroductionConflict(element: CjCasePatternElement) {
            trace.report(VARIABLE_INTRODUCTION_CONFLICT.on(element))
        }

        val connectorVisitor = object : CjVisitorUnit() {
            override fun visitPatternByType(element: CjTypePattern) {
                reportVariableIntroductionConflict(element)
            }

            override fun visitPatternByEnum(element: CjEnumPattern) {
                element.patterns.forEach { it.accept(this) }
            }

            override fun visitPatternByTuple(element: CjTuplePattern) {
                element.patterns.forEach { it.accept(this) }
            }

            override fun visitPatternByBinding(element: CjBindingPattern) {
                if (bindingContext[VARIABLE, element] != null) {
                    reportVariableIntroductionConflict(element)
                }
            }
        }

        for (matchEntry in matchExpression.entries) {
            if (matchEntry.conditions.size < 2) continue
            for (condition in matchEntry.conditions) {
                condition.accept(connectorVisitor)
            }
        }
    }


    // ===== 工具方法 =====

    /**
     * 获取 match 主体类型
     */
    fun getMatchSubjectType(expression: CjMatchExpression, context: BindingContext): CangJieType? {
        val subjectExpression = expression.subjectExpression
        return when {
            subjectExpression != null -> context[SMARTCAST, subjectExpression]?.defaultType
                ?: context.getType(subjectExpression)
            else -> null
        }
    }

    /**
     * 检查字面量模式
     */
    fun checkLiteralPattern(
        matchExpression: CjMatchExpression,
        subjectType: CangJieType?,
        context: BindingContext
    ): Boolean {
        subjectType ?: return false
        return when {
            subjectType.isBoolean -> {
                val booleanValues = matchExpression.entries.flatMap {
                    it.conditions.filter { condition ->
                        condition is CjConstantPattern &&
                                condition.expression is CjConstantExpression &&
                                condition.expression?.elementType == BOOLEAN_CONSTANT
                    }.map { pattern -> pattern.text }
                }
                booleanValues.contains("true") && booleanValues.contains("false")
            }
            subjectType.isUnit() -> {
                matchExpression.entries.any {
                    it.conditions.any { condition ->
                        condition is CjConstantPattern &&
                                condition.expression is CjConstantExpression &&
                                condition.expression?.elementType == UNIT_CONSTANT
                    }
                }
            }
            else -> false
        }
    }

    companion object {
        /**
         * 获取类型对应的密封类描述符
         */
        fun getClassDescriptorOfTypeIfSealed(type: CangJieType?): org.cangnova.cangjie.descriptors.ClassDescriptor? =
            type?.let { TypeUtils.getClassDescriptor(it) }?.takeIf { DescriptorUtils.isSealedClass(it) }

        /**
         * 获取类型对应的元组描述符
         */
        fun getClassDescriptorOfTypeIfTuple(type: CangJieType?): org.cangnova.cangjie.descriptors.ClassDescriptor? {
            if (type == null) return null
            val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
            if (classDescriptor.kind != ClassKind.TUPLE) return null
            return classDescriptor
        }

        /**
         * 获取类型对应的枚举描述符
         */
        fun getClassDescriptorOfTypeIfEnum(type: CangJieType?): org.cangnova.cangjie.descriptors.ClassDescriptor? {
            if (type == null) return null
            val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
            if (classDescriptor.kind != ClassKind.ENUM) return null
            return classDescriptor
        }
        /**
         * 检查重复标签
         */
        fun checkDuplicatedLabels(
            expression: CjMatchExpression,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
        ) {
            // TODO: 实现重复标签检查
        }

    }


}
