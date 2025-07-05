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

package cn.cangnova.cangjie.types.expressions.match

import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.StandardNames.ITERABLE
import cn.cangnova.cangjie.builtins.isBuiltinTupleType
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.enumd.EnumEntryDescriptor
import cn.cangnova.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import cn.cangnova.cangjie.diagnostics.Errors.*
import cn.cangnova.cangjie.diagnostics.MatchMissingCase
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.*
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.elementType
import cn.cangnova.cangjie.psi.psiUtil.referenceExpression
import cn.cangnova.cangjie.resolve.*
import cn.cangnova.cangjie.resolve.BindingContext.*
import cn.cangnova.cangjie.resolve.DescriptorResolver.Companion.getDefaultVisibility
import cn.cangnova.cangjie.resolve.DescriptorUtils.isEnum
import cn.cangnova.cangjie.resolve.ModifiersChecker.Companion.resolveVisibilityFromModifiers
import cn.cangnova.cangjie.resolve.caches.ConfusingMatchBranchSyntaxChecker
import cn.cangnova.cangjie.resolve.caches.PrimitiveNumericComparisonCallChecker
import cn.cangnova.cangjie.resolve.calls.checkers.RttiExpressionInformation
import cn.cangnova.cangjie.resolve.calls.checkers.RttiOperation
import cn.cangnova.cangjie.resolve.calls.context.ContextDependency
import cn.cangnova.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import cn.cangnova.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import cn.cangnova.cangjie.resolve.descriptorUtil.classId
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import cn.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import cn.cangnova.cangjie.resolve.source.getPsi
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import cn.cangnova.cangjie.types.error.ErrorTypeKind
import cn.cangnova.cangjie.types.expressions.*
import cn.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import cn.cangnova.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfMatchCall
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import cn.cangnova.cangjie.types.util.*
import cn.cangnova.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import cn.cangnova.cangjie.utils.addIfNotNull
import cn.cangnova.cangjie.utils.runIf
import java.util.*

sealed class Subject(
    val element: CjElement?,
    val typeInfo: CangJieTypeInfo?,
    val scopeWithSubject: LexicalScope?,
    var type: CangJieType = typeInfo?.type ?: ErrorUtils.createErrorType(ErrorTypeKind.UNKNOWN_TYPE)
) {


    protected abstract fun createDataFlowValue(
        contextAfterSubject: ExpressionTypingContext,
        builtIns: CangJieBuiltIns
    ): DataFlowValue

    abstract fun makeValueArgument(): ValueArgument?
    abstract val valueExpression: CjExpression?
    open fun getCalleeExpressionForSpecialCall(): CjExpression? = null
    lateinit var dataFlowValue: DataFlowValue; protected set
    fun initDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) {
        dataFlowValue = createDataFlowValue(contextAfterSubject, builtIns)
    }

    val dataFlowInfo get() = typeInfo?.dataFlowInfo

    val jumpOutPossible get() = typeInfo?.jumpOutPossible ?: false

    class None : Subject(null, null, null) {
        override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) =
            DataFlowValue.nullValue(builtIns)

        override fun makeValueArgument(): ValueArgument? = null

        override val valueExpression: CjExpression? get() = null
    }

    class Type(typeInfo: CangJieTypeInfo, _dataFlowValue: DataFlowValue?, context: ExpressionTypingContext) :
        Subject(null, typeInfo, null) {


        init {
            if (_dataFlowValue != null) {
                dataFlowValue = _dataFlowValue
            }
        }

        override fun createDataFlowValue(
            contextAfterSubject: ExpressionTypingContext,
            builtIns: CangJieBuiltIns
        ): DataFlowValue {
            return if (::dataFlowValue.isInitialized) {
                dataFlowValue
            } else {
                DataFlowValue.nullValue(builtIns)

            }
        }

        override fun makeValueArgument(): ValueArgument? {
            return null
        }

        override val valueExpression: CjExpression? = null

    }

    class Expression(
        val expression: CjExpression,
        typeInfo: CangJieTypeInfo,
        private val dataFlowValueFactory: DataFlowValueFactory
    ) : Subject(expression, typeInfo, null) {
        override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: CangJieBuiltIns) =
            dataFlowValueFactory.createDataFlowValue(expression, type, contextAfterSubject)

        override fun makeValueArgument(): ValueArgument =
            CallMaker.makeExternalValueArgument(expression)

        override val valueExpression: CjExpression
            get() = expression
    }

}

interface ClassAndEnumConstructorDescriptor
class TupleConstructor(val types: List<CangJieType>) : ClassAndEnumConstructorDescriptor


class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) :
    ExpressionTypingVisitor(facade) {


    override fun visitVariable(variable: CjVariable, typingContext: ExpressionTypingContext): CangJieTypeInfo {
        // 更新上下文依赖关系和作用域
        val context = typingContext.replaceContextDependency(ContextDependency.INDEPENDENT)
        val visibility =
            resolveVisibilityFromModifiers(
                variable,
                getDefaultVisibility(variable, typingContext.scope.ownerDescriptor)
            )

        // 检查接收器类型引用并报告诊断信息
        val receiverTypeRef = variable.receiverTypeReference
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_VARIABLE.on(receiverTypeRef))
        }

//        模式
        val pattern = variable.pattern

//        预期的类型
        val typeReference = variable.typeReference
        val expectedType = typeReference?.let {
            facade.components.typeResolver.resolveType(
                context.scope,
                it,
                context.trace,
                true
            )
        }

        // 处理变量初始化
        val initializer = variable.initializer
//表达式类型
        val initializerTypeInfo =
            initializer?.let { facade.getTypeInfo(it, context.replaceExpectedType(expectedType)) } ?: noTypeInfo(
                typingContext
            )
        val subject = when {

            initializer != null ->
                Subject.Expression(
                    initializer,
                    expectedType?.let { createTypeInfo(it) } ?: initializerTypeInfo,
                    components.dataFlowValueFactory
                )

            expectedType != null -> {
                Subject.Type(
                    createTypeInfo(expectedType),
                    null, context
                )
            }

            else ->
                Subject.None()
        }
        val contextAfterSubject = run {
            var result = context
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }

        subject.initDataFlowValue(contextAfterSubject, components.builtIns)

        pattern?.let {
            resoleCasePattern(
                subject,
                it,
                context,
                Config(
                    isVar = variable.isVar,
                    bindEnumEntry = false,
                    isLocal = variable.isLocal,
                    visibility = visibility
                )

            )

            isOverwriteForVariableDeclaration(pattern, initializer, context.trace)
        }


        return createTypeInfo(components.builtIns.unitType)
    }

    override fun visitLetExpression(
        expression: CjLetExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        if (expression.pattern is CjTypePattern) {

            context.trace.report(LET_EXPRESSION_NO_TYPE_PATTERN.on(expression.pattern))
//            return noTypeInfo(context)

        }
        val subjectExpression = expression.expression


        val subject = when {

            subjectExpression != null ->
                Subject.Expression(
                    subjectExpression,
                    facade.getTypeInfo(subjectExpression, context),
                    components.dataFlowValueFactory
                )

            else ->
                Subject.None()
        }

        val contextAfterSubject = run {
            var result = context
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }

        subject.initDataFlowValue(contextAfterSubject, components.builtIns)

        expression.pattern?.let { resoleCasePattern(subject, it, contextAfterSubject) }
        return noTypeInfo(context)
    }

    fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePattern,
        receiver: ReceiverValue,
        initializer: CjExpression?,
        context: ExpressionTypingContext
    ) {

        initializer ?: return
        if(receiver.type.isError) return
//        获取原始迭代器对象
        val iterator = receiver.type.extractSuperType(ITERABLE)
//        被迭代对象
        val iteratedType = iterator.arguments.first().type


        val subject =
            Subject.Expression(initializer, createTypeInfo(iteratedType), context.dataFlowValueFactory).apply {
                initDataFlowValue(context, components.builtIns)
            }

        resoleCasePattern(
            subject, casePattern, context.replaceScope(writableScope), Config(false)
        )


        isOverwriteForForInExpr(casePattern, initializer, context.trace)

    }

    override fun visitMatchExpression(
        expression: CjMatchExpression,
        data: ExpressionTypingContext
    ): CangJieTypeInfo =
        visitMatchExpression(expression, data, false)

    fun visitMatchExpression(
        expression: CjMatchExpression,
        contextWithExpectedType: ExpressionTypingContext,
        @Suppress("UNUSED_PARAMETER") isStatement: Boolean
    ): CangJieTypeInfo {
        val trace = contextWithExpectedType.trace
        MatchChecker.checkDeprecatedMatchSyntax(trace, expression)

        components.dataFlowAnalyzer.recordExpectedType(trace, expression, contextWithExpectedType.expectedType)
        val contextBeforeSubject =
            contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)


//根据match标头中的绑定值更改范围
        val subjectExpression = expression.subjectExpression

        val subject = when {

            subjectExpression != null ->
                Subject.Expression(
                    subjectExpression,
                    facade.getTypeInfo(subjectExpression, contextBeforeSubject),
                    components.dataFlowValueFactory
                )

            else ->
                Subject.None()
        }

//        val contextAfterSubject = run {
//            var result = contextBeforeSubject
//            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
//            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
//            result
//        }


        val matchScope =
            ExpressionTypingUtils.newWritableScopeImpl(
                contextWithExpectedType,
                LexicalScopeKind.MATCH,
                components.overloadChecker
            )
        var matchContext = contextWithExpectedType.replaceScope(matchScope)

        val usedAsExpression = expression.isUsedAsExpression(trace.bindingContext)

//        if(matchContext.expectedType == NO_EXPECTED_TYPE){
//            matchContext = matchContext.replaceExpectedType(components.builtIns.anyType)
//        }
        subject.initDataFlowValue(matchContext, components.builtIns)
        val possibleTypesForSubject =
            subject.typeInfo?.dataFlowInfo?.getStableTypes(subject.dataFlowValue, components.languageVersionSettings)
                ?: emptySet()

//        checkSmartCastsInSubjectIfRequired(expression, contextBeforeSubject, subject.type, possibleTypesForSubject)


//        if (subject is Subject.Expression) {
//            checkExhaustive(subject, expression, matchContext)
//        }

        val dataFlowInfoForEntries = analyzeConditionsInMatchEntries(expression, matchContext, subject)
        val matchReturnType = inferTypeForMatchExpression(
            expression,
            subject,
            matchContext,
            matchContext,
            dataFlowInfoForEntries
        )
        val matchResultValue =
            matchReturnType?.let {
                facade.components.dataFlowValueFactory.createDataFlowValue(
                    expression,
                    it,
                    matchContext
                )
            }

        val branchesTypeInfo =
            joinMatchExpressionBranches(
                expression,
                matchContext,
                matchReturnType,
                subject.jumpOutPossible,
                matchResultValue
            )

        val isExhaustive = MatchChecker.isMatchExhaustive(expression, trace)

        val branchesDataFlowInfo = branchesTypeInfo.dataFlowInfo
        val resultDataFlowInfo = if (expression.elseExpression == null && !isExhaustive) {
            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
            // because data flow can bypass all when branches in this case
            branchesDataFlowInfo.or(matchContext.dataFlowInfo)
        } else {
            branchesDataFlowInfo
        }

        if (matchReturnType != null && isExhaustive && expression.elseExpression == null && CangJieBuiltIns.isNothing(
                matchReturnType
            )
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

            val entryDataFlowInfo =
                if (whenResultValue != null && entryType != null) {
                    val entryValue =
                        facade.components.dataFlowValueFactory.createDataFlowValue(
                            entryExpression,
                            entryType,
                            contextAfterSubject
                        )
                    entryTypeInfo.dataFlowInfo.assign(
                        whenResultValue,
                        entryValue/*, components.languageVersionSettings*/
                    )
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
                override fun get(index: Int): String = "entry$index"
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

    private fun noChange(context: ExpressionTypingContext) = ConditionalDataFlowInfo(context.dataFlowInfo)

    data class Config(
        val bindEnumType: Boolean = true,
        val isVar: Boolean = false,
        val isLocal: Boolean = true,
//        绑定模式解析枚举
        val bindEnumEntry: Boolean = true,
        val visibility: DescriptorVisibility? = null

    )

    private fun resoleCasePattern(
        subject: Subject,
        condition: CjCasePattern,
        context: ExpressionTypingContext,
        config: Config = Config()
    ): ConditionalDataFlowInfo {

        var newDataFlowInfo = noChange(context)

        val patternVisitor = object : CjVisitorVoid() {
            val pattern = PatternVisitor(
                config
            )

            override fun visitMatchConditionWithExpression(element: CjMatchConditionWithExpression) {
                val expression = element.expression ?: return

                val basicDataFlowInfo =
                    checkTypeForExpressionCondition(context, expression, subject)
                val moduleDescriptor = DescriptorUtils.getContainingModule(context.scope.ownerDescriptor)
                val dataFlowInfoFromES =
                    components.effectSystem.getDataFlowInfoMatchEquals(
                        subject.valueExpression,
                        expression,
                        context.trace,
                        moduleDescriptor
                    )
                newDataFlowInfo = basicDataFlowInfo.and(dataFlowInfoFromES)
            }

            override fun visitPatternByEnum(element: CjEnumPattern) {
                pattern.visitPatternByEnum(element, PatternContext(subject, context))
            }

            override fun visitPatternByType(element: CjTypePattern) {
                pattern.visitPatternByType(element, PatternContext(subject, context))
            }

            override fun visitPatternByTuple(element: CjTuplePattern) {
                pattern.visitPatternByTuple(element, PatternContext(subject, context))
            }

            override fun visitPatternByBinding(element: CjBindingPattern) {
                pattern.visitPatternByBinding(element, PatternContext(subject, context))
            }

            override fun visitPatternByConstant(element: CjConstantPattern) {
                pattern.visitPatternByConstant(element, PatternContext(subject, context))
            }

            override fun visitPatternByWildcard(element: CjWildcardPattern) {
                pattern.visitPatternByWildcard(element, PatternContext(subject, context))

            }
        }

        condition.accept(patternVisitor)
        return newDataFlowInfo

    }

    //    private fun CjCasePattern.getKind(context: ExpressionTypingContext): PatternKind {
//
//    }
    private fun checkExhaustive(
        subject: Subject,
        expr: CjMatchExpression,
        context: ExpressionTypingContext,
        config: Config = Config()
    ) {
        val patternVisitor = PatternVisitor()

        val a = expr.entries.flatMap { entry ->
            entry.conditions.map {
                it.accept(patternVisitor, PatternContext(subject, context))
            }

        }

        a
//        val matrix = expr.entries.
//
//        calculateMatrix()

    }

    private fun analyzeMatchEntryConditions(
        matchEntry: CjMatchEntry,
        context: ExpressionTypingContext,
        subject: Subject
    ): ConditionalDataFlowInfo {
        if (matchEntry.isElse) {
            return ConditionalDataFlowInfo(context.dataFlowInfo)
        }

        var entryInfo: ConditionalDataFlowInfo? = null

        val caseScope =
            ExpressionTypingUtils.newWritableScopeImpl(
                context,
                LexicalScopeKind.MATCH_CASE,
                components.overloadChecker
            )
        var contextForCondition = context.replaceScope(caseScope)


        for (condition in matchEntry.conditions) {
            val conditionInfo = resoleCasePattern(subject, condition, contextForCondition)
            entryInfo = entryInfo?.let {
                ConditionalDataFlowInfo(it.thenInfo.or(conditionInfo.thenInfo), it.elseInfo.and(conditionInfo.elseInfo))
            } ?: conditionInfo

            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfo.elseInfo)
        }
        context.trace.recordScope(caseScope, matchEntry.body)

        return entryInfo ?: ConditionalDataFlowInfo(context.dataFlowInfo)
    }

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

            matchEntry.patternGuard?.expression?.let {
                facade.getTypeInfo(
                    it,
                    contextAfterSubject.replaceExpectedType(components.builtIns.boolType)
                )
            }

        }
        return argumentDataFlowInfos
    }

    //    类型智能转换
    private fun checkSmartCastsInSubjectIfRequired(
        expression: CjMatchExpression,
        contextBeforeSubject: ExpressionTypingContext,
        subjectType: CangJieType,
        possibleTypesForSubject: Set<CangJieType>
    ) {

    }


    private fun checkTypeForIs(
        context: ExpressionTypingContext,
        isCheck: CjElement,

        subjectType: CangJieType,
        typeReferenceAfterIs: CjTypeReference,
        subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        val typeResolutionContext =
            TypeResolutionContext(
                context.scope,
                context.trace,
                true, /*allowBareTypes=*/
                true,
                context.isDebuggerContext
            )
        val possiblyBareTarget =
            components.typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs)
        val targetType = TypeReconstructionUtil.reconstructBareType(
            typeReferenceAfterIs,
            possiblyBareTarget,
            subjectType,
            context.trace,
            components.builtIns
        )

        if (targetType.isDynamic()) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs))
        }
        val targetDescriptor = TypeUtils.getClassDescriptor(targetType)
        if (targetDescriptor != null && DescriptorUtils.isEnumEntry(targetDescriptor)) {
            context.trace.report(IS_ENUM_ENTRY.on(typeReferenceAfterIs))
        }
        if (!subjectType.containsError() && !TypeUtils.isNullableType(subjectType) && targetType.isMarkedOption) {
            val element = typeReferenceAfterIs.typeElement
            assert(element is CjOptionType) { "element must be instance of " + CjOptionType::class.java.name }
            context.trace.report(USELESS_NULLABLE_CHECK.on(element as CjOptionType))
        }
        val typesAreCompatible = checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs)

        detectRedundantIs(context, subjectType, targetType, isCheck, subjectDataFlowValue, typesAreCompatible)

//        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperCheckAnnotationsTargetInTypeUsePositions)) {
//            components.annotationChecker.check(typeReferenceAfterIs, context.trace)
//        }

//        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, CangJieTypeChecker.DEFAULT)) {
//            context.trace.report(CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType))
//        }
        return context.dataFlowInfo.let {
            ConditionalDataFlowInfo(
                it.establishSubtyping(
                    subjectDataFlowValue,
                    targetType,
                    components.languageVersionSettings
                ), it
            )
        }
    }

    //    检查冗余的is检查
    private fun detectRedundantIs(
        context: ExpressionTypingContext,
        subjectType: CangJieType,
        targetType: CangJieType,
        isCheck: CjElement,

        subjectDataFlowValue: DataFlowValue,
        typesAreCompatible: Boolean
    ) {
        if (subjectType.containsError() || targetType.containsError()) return

        val possibleTypes =
            DataFlowAnalyzer.getAllPossibleTypes(
                subjectType,
                context,
                subjectDataFlowValue,
                context.languageVersionSettings
            )

//        if (typesAreCompatible && !targetType.isError) {
//            val nonTrivialTypes = possibleTypes.filterNot { it.isAny() }
//                .takeIf { it.isNotEmpty() }
//                ?: possibleTypes
//
//            if (nonTrivialTypes.none {
//                    CastDiagnosticsUtil.isCastPossible(
//                        it,
//                        targetType, components.platformToCangJieClassMapper,/* components.platformSpecificCastChecker*/
//                    )
//                }) {
//                context.trace.report(USELESS_IS_CHECK.on(isCheck, false))
//            }
//        }
        if (!typesAreCompatible && !targetType.isError) {
//            val nonTrivialTypes = possibleTypes.filterNot { it.isAny() }
//                .takeIf { it.isNotEmpty() }
//                ?: possibleTypes
//
//            if (nonTrivialTypes.none {
//                    CastDiagnosticsUtil.isCastPossible(
//                        it,
//                        targetType, components.platformToCangJieClassMapper,/* components.platformSpecificCastChecker*/
//                    )
//                }) {
            context.trace.report(USELESS_IS_CHECK.on(isCheck, false))
//            }
        } else

            if (CastDiagnosticsUtil.isRefinementUseless(possibleTypes, targetType, false)) {
                context.trace.report(USELESS_IS_CHECK.on(isCheck, true))
            }
    }

    override fun visitIsExpression(
        expression: CjIsExpression,
        contextWithExpectedType: ExpressionTypingContext
    ): CangJieTypeInfo {
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
            ContextDependency.INDEPENDENT
        )
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

//        expression.reportDeprecatedDefinitelyNotNullSyntax(expression.typeReference, contextWithExpectedType)

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

    private fun checkTypeForExpressionCondition(
        context: ExpressionTypingContext,
        expression: CjExpression,
        subject: Subject
    ): ConditionalDataFlowInfo {
        var newContext = context
        val typeInfo = facade.getTypeInfo(expression, newContext)
        val type = typeInfo.type ?: return noChange(newContext)
        newContext = newContext.replaceDataFlowInfo(typeInfo.dataFlowInfo)

        if (subject is Subject.None) { // condition expected
            val booleanType = components.builtIns.boolType
            val checkedTypeInfo =
                components.dataFlowAnalyzer.checkType(typeInfo, expression, newContext.replaceExpectedType(booleanType))
            if (CangJieTypeChecker.DEFAULT.equalTypes(booleanType, checkedTypeInfo.type ?: type)) {
                val ifInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, true, newContext)
                val elseInfo =
                    components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, false, newContext)
                return ConditionalDataFlowInfo(ifInfo, elseInfo)
            }
            return noChange(newContext)
        }

        checkTypeCompatibility(newContext, type, subject.type, expression, true)
        val expressionDataFlowValue =
            facade.components.dataFlowValueFactory.createDataFlowValue(expression, type, newContext)

        val subjectStableTypes =
            listOf(subject.type) + context.dataFlowInfo.getStableTypes(
                subject.dataFlowValue,
                components.languageVersionSettings
            )
        val expressionStableTypes =
            listOf(type) + newContext.dataFlowInfo.getStableTypes(
                expressionDataFlowValue,
                components.languageVersionSettings
            )
        PrimitiveNumericComparisonCallChecker.inferPrimitiveNumericComparisonType(
            context.trace,
            subjectStableTypes,
            expressionStableTypes,
            expression
        )

        val result = noChange(newContext)
        return ConditionalDataFlowInfo(
            result.thenInfo.equate(
                subject.dataFlowValue, expressionDataFlowValue,
                identityEquals = facade.components.dataFlowAnalyzer.typeHasEqualsFromAny(subject.type, expression),
                languageVersionSettings = components.languageVersionSettings
            ),
            result.elseInfo.disequate(
                subject.dataFlowValue,
                expressionDataFlowValue,
                components.languageVersionSettings
            )
        )
    }

    /**
     * 检查类型兼容
     * (a: SubjectType) is Type
     */
    private fun checkTypeCompatibility(
        context: ExpressionTypingContext,
        type: CangJieType,
        subjectType: CangJieType,
        reportErrorOn: CjElement,
        isReportError: Boolean = false
    ): Boolean {
        // TODO : Take smart casts into account?
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
            if (isReportError) {
                context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType))

            }
//            context.trace.report(USELESS_IS_CHECK.on(reportErrorOn, false))

            return false
        }

        checkEnumsForCompatibility(context, reportErrorOn, subjectType, type)

        // check if the pattern is essentially a 'null' expression
        if (CangJieBuiltIns.isNothing(type) && !TypeUtils.isNullableType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_MATCH.on(reportErrorOn))
        }
        return true
    }


    inner class PatternVisitor(
        val config: Config = Config()

    ) : CjVisitor<Pattern, PatternContext>() {

        private fun returnResult(element: CjCasePattern, data: PatternContext, result: Pattern): Pattern {


            data.context.trace.record(PATTERN, element, result)

            return result
        }

        override fun visitPatternByConstant(element: CjConstantPattern, data: PatternContext): Pattern {
            val expression = element.expression!!
            val newContext = data.context
            val typeInfo = facade.getTypeInfo(expression, newContext)
            checkTypeCompatibility(newContext, typeInfo.type!!, data.subject.type, expression, true)

            return returnResult(
                element, data, Pattern(
                    data.subject.type,
                    PatternKind.Const(
                        newContext.trace[COMPILE_TIME_VALUE, expression]!!.toConstantValue(typeInfo.type)
                    )
                )
            )


        }

        override fun visitPatternByEnum(element: CjEnumPattern, data: PatternContext): Pattern {
//components.callExpressionResolver.getSimpleNameExpressionTypeInfo

            val expression = element.expression

            val typeInfo = if (data.subject.type.isEnum()) {
                expression?.let {
                    facade.getTypeInfoByCaseEnum(
                        it,
                        element.patterns,
                        data.context.replaceExpectedType(data.subject.type)
                    )
                }
            } else {
                expression?.let { facade.getTypeInfoByCaseEnum(it, element.patterns, data.context) }
            }
            if (typeInfo?.type == null) {
//               如果type未空，一定不匹配
                data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
            } else if (!CangJieTypeChecker.DEFAULT.equalTypes(typeInfo.type, data.subject.type)) {
                data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
            }

            typeInfo?.type ?: return returnResult(element, data, Pattern.Error)
            val enumSource = typeInfo.type.deccriptorClass?.source?.getPsi() as? CjEnum ?: return returnResult(
                element, data, Pattern(
                    typeInfo.type,
                    PatternKind.Error
                )
            )

            val enumEntry =
                data.context.trace[REFERENCE_TARGET, expression?.referenceExpression()] as? EnumClassCallableDescriptor
            val enumEntrySource =
                enumEntry?.toSourceElement?.getPsi() as? CjEnumEntry
                    ?: return returnResult(
                        element, data, Pattern(typeInfo.type, PatternKind.Error)
                    )

            val valueParameters = enumEntry.valueParameters

            return try {
                returnResult(
                    element, data, Pattern(
                        typeInfo.type, PatternKind.Enum(
                            enumSource, enumEntrySource,
                            valueParameters.mapIndexed { index, it ->
//                        期望类型
                                val expectedType = it.type
                                element.patterns[index].accept(
                                    this, PatternContext(
                                        Subject.Expression(
                                            element.patterns[index],
                                            createTypeInfo(expectedType),
                                            components.dataFlowValueFactory
                                        ),
                                        data.context
                                    )
                                )
                            }
                        ))
                )
            } catch (e: IndexOutOfBoundsException) {
                returnResult(element, data, Pattern.Error)
            }

        }

        //            类型模式
        override fun visitPatternByType(element: CjTypePattern, data: PatternContext): Pattern {
            val type =
                element.typeReference?.let {
                    components.typeResolver.resolveType(
                        data.context.scope,
                        it, data.context.trace, false
                    )
                } ?: ErrorUtils.errorVariableType


            val variable = if (config.isLocal) {
                components.localVariableResolver.resolveLocalVariableDescriptorWithType(
                    data.context.scope, element, type, data.context.trace, config.isVar
                )
            } else {
                components.localVariableResolver.resolveVariableDescriptorWithType(
                    data.context.scope, element, data.subject.type, data.context.trace, config.isVar,config.visibility
                )
            }

            data.context.scope.addVariableDescriptor(variable)


            return returnResult(
                element, data, Pattern(data.subject.type, PatternKind.Type(type, element.text))
            )
        }

        override fun visitPatternByTuple(element: CjTuplePattern, data: PatternContext): Pattern {


            val patterns = element.patterns
            val patternSize = patterns.size

            if (!data.subject.type.isBuiltinTupleType) {
                data.context.trace.report(TUPLE_PATTERN_TYPE_MISMATCH.on(element, data.subject.type))
                return returnResult(
                    element, data, Pattern.Error
                )
            }

            if (patternSize < 1) {
                data.context.trace.report(TUPLE_ARGS_TOO_FEW.on(element))
                return returnResult(
                    element, data, Pattern.Error
                )
            }
            if (patternSize != data.subject.type.arguments.size) {
                data.context.trace.report(
                    TUPLE_ARGS_MISMATCH.on(
                        element,
                        data.subject.type.arguments.size,
                        patternSize
                    )
                )
                return returnResult(
                    element, data, Pattern.Error
                )
            }
//            data.subject.type.arguments.forEachIndexed { index, argumentType ->
//                resoleCasePattern(
//                    Subject.Type(
//
//                        createTypeInfo(argumentType.type, data.context),
//                        data.subject.dataFlowValue, data.context
//                    ),
//                    patterns[index],
//                    data.context
//                )
//
//            }

            return returnResult(
                element, data, Pattern(data.subject.type,
                    PatternKind.Tuple(
                        element.patterns.mapIndexed { index, it ->
//                        期望类型
                            val expectedType = data.subject.type.arguments[index].type
                            it.accept(
                                this, PatternContext(
                                    Subject.Expression(
                                        it,
                                        createTypeInfo(expectedType),
                                        components.dataFlowValueFactory
                                    ),
                                    data.context
                                )
                            )
                        }
                    ))
            )


        }

        override fun visitPatternByWildcard(element: CjWildcardPattern, data: PatternContext): Pattern {
            return returnResult(
                element, data, Pattern(
                    data.subject.type,
                    PatternKind.Wild
                )
            )
        }

        override fun visitPatternByBinding(element: CjBindingPattern, data: PatternContext): Pattern {

            val expression = element.expression

            if (config.bindEnumEntry) {
                val typeInfo = if (data.subject.type.isEnum()) {
                    expression?.let {
                        facade.getTypeInfoByCaseEnum(
                            it,
                            emptyList(),
                            data.context.replaceExpectedType(data.subject.type), false
                        )
                    }
                } else {
                    expression?.let { facade.getTypeInfoByCaseEnum(it, emptyList(), data.context, false) }
                }
                if (typeInfo?.type != null) {
                    if (!CangJieTypeChecker.DEFAULT.equalTypes(typeInfo.type, data.subject.type)) {
                        data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                    }
                    val enumSource = typeInfo.type.deccriptorClass?.source?.getPsi() as? CjEnum ?: return returnResult(
                        element, data, Pattern(
                            typeInfo.type,
                            PatternKind.Error
                        )
                    )

                    val enumEntry =
                        data.context.trace[REFERENCE_TARGET, expression?.referenceExpression()] as? EnumClassCallableDescriptor
                    val enumEntrySource =
                        enumEntry?.toSourceElement?.getPsi() as? CjEnumEntry
                            ?: return returnResult(
                                element, data, Pattern(typeInfo.type, PatternKind.Error)
                            )


                    return returnResult(
                        element, data, Pattern(
                            typeInfo.type, PatternKind.Enum(
                                enumSource, enumEntrySource,

                                emptyList()

                            )
                        )
                    )

                } else if (expression?.getReferenceTarget(data.context.trace.bindingContext) is EnumClassCallableDescriptor) {
                    data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                    return returnResult(
                        element, data, Pattern.Error
                    )
                }

            }

// TODO 将变量添加到作用域  这里需要架构重构，目前这个写的并不理想
            val variable = if (config.isLocal) {
                components.localVariableResolver.resolveLocalVariableDescriptorWithType(
                    data.context.scope, element, data.subject.type, data.context.trace, config.isVar
                )
            } else {
                components.localVariableResolver.resolveVariableDescriptorWithType(
                    data.context.scope, element, data.subject.type, data.context.trace, config.isVar,config.visibility
                )
            }

            data.context.scope.addVariableDescriptor(variable)



            return returnResult(
                element, data, Pattern(data.subject.type, PatternKind.Binding(data.subject.type, element.text))
            )
        }
    }

}

private interface MatchExhaustivenessChecker {
    fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        type: CangJieType?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        return emptyList()
    }

    fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<MatchMissingCase>

    fun isOverwrite(
        pattern: CjCasePattern,

        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,


        ): Boolean

    fun isApplicable(subjectType: CangJieType): Boolean = false
}


fun CjMatchExpression.checkExhaustive(context: BindingContext): List<Pattern>? {

    return doCheckExhaustive(this, context)
}

object MatchChecker {
    @JvmStatic
    fun getClassDescriptorOfTypeIfSealed(type: CangJieType?): ClassDescriptor? =
        type?.let { TypeUtils.getClassDescriptor(it) }?.takeIf { DescriptorUtils.isSealedClass(it) }

    @JvmStatic
    fun getClassDescriptorOfTypeIfTuple(type: CangJieType?): ClassDescriptor? {
        if (type == null) return null
        val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
        if (classDescriptor.kind != ClassKind.TUPLE) return null

        return classDescriptor
    }

    @JvmStatic
    fun getClassDescriptorOfTypeIfEnum(type: CangJieType?): ClassDescriptor? {
        if (type == null) return null
        var classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
        if (classDescriptor.kind == ClassKind.ENUM_ENTRY) {
            classDescriptor = classDescriptor.containingDeclaration as ClassDescriptor
        }
        if (classDescriptor.kind != ClassKind.ENUM) return null

        return classDescriptor
    }

    @JvmStatic
    fun matchSubjectType(expression: CjMatchExpression, context: BindingContext): CangJieType? {
//        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        return when {
//            subjectVariable != null -> context.get(VARIABLE, subjectVariable)?.type
            subjectExpression != null -> context.get(SMARTCAST, subjectExpression)?.defaultType ?: context.getType(
                subjectExpression
            )

            else -> null
        }
    }

    private val exhaustivenessCheckers: List<MatchExhaustivenessChecker> = listOf(
//        MatchOnBooleanExhaustivenessChecker,
        MatchOnEnumExhaustivenessChecker,
        MatchOnTupleExhaustivenessChecker,
        MatchOnOtherExhaustivenessChecker,
//        MatchOnSealedExhaustivenessChecker
    )

    @Deprecated("use isOverwriteForForInExpr")
    fun isOverwrite(pattern: CjCasePattern, type: CangJieType, context: BindingContext): Boolean {
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type) }
        if (checkers.isEmpty()) return false
        return checkers.all {
            it.isOverwrite(
                pattern,

                context,
                TypeUtils.getClassDescriptor(type)
            )
        }
    }


    fun getMissingCases(expression: CjMatchExpression, context: BindingContext): List<MatchMissingCase> {
        val type = MatchSubjectType(expression, context) ?: return listOf(MatchMissingCase.Unknown)
        val nullable = type.isMarkedOption
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type) }
        if (checkers.isEmpty()) return listOf(MatchMissingCase.Unknown)
        return checkers.map {
            it.getMissingCases(
                expression,
                context,
                TypeUtils.getClassDescriptor(type),
                nullable
            ) + it.getMissingCases(expression, context, type, nullable)
        }
            .flatten()
    }


    @JvmStatic
    fun MatchSubjectType(expression: CjMatchExpression, context: BindingContext): CangJieType? {
//        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        val type = when {
//            subjectVariable != null -> context.get(VARIABLE, subjectVariable)?.type
            subjectExpression != null -> context.get(SMARTCAST, subjectExpression)?.defaultType ?: context.getType(
                subjectExpression
            )

            else -> null
        }
        return if (type?.isEnumEntry() == true) {
            return (type.constructor.declarationDescriptor?.containingDeclaration as? ClassDescriptor)?.defaultType
        } else {
            type
        }

    }

    @JvmStatic
    fun isMatchExhaustive(expression: CjMatchExpression, trace: BindingTrace) = false

    //        if (getMissingCases(expression, trace.bindingContext).isEmpty()) {
//            trace.record(BindingContext.EXHAUSTIVE_WHEN, expression)
//            true
//        } else {
//            false
//        }
    //    检查没有条件的match表达式的语法  match{}
    fun checkDeprecatedMatchSyntax(trace: BindingTrace, expression: CjMatchExpression) {
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

    fun checkDuplicatedLabels(
        expression: CjMatchExpression,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
    ) {
//        if (expression.subjectExpression == null) return
//
//        val checkedTypes = HashSet<Pair<CangJieType, Boolean>>()
//        val checkedConstants = mutableMapOf<CompileTimeConstant<*>, Boolean>()
//        val notTrivialBranches = mutableMapOf<CompileTimeConstant<*>, CjExpression>()
//        for (entry in expression.entries) {
//            if (entry.isElse) continue
//
//            conditions@ for (condition in entry.conditions) {
//                when (condition) {
//                    is CjMatchConditionWithExpression -> {
//                        val constantExpression = condition.expression ?: continue@conditions
//                        val constant = ConstantExpressionEvaluator.getConstant(
//                            constantExpression, trace.bindingContext
//                        ) ?: continue@conditions
//
//                        fun report(reportOn: CjExpression) {
//                            trace.report(Errors.DUPLICATE_LABEL_IN_MATCH.on(reportOn))
//                        }
//
//                        when (checkedConstants[constant]) {
//                            true -> {
//                                // already found trivial constant in previous branches
//                                report(constantExpression)
//                            }
//                            false -> {
//                                // already found bad constant in previous branches
//                                val isTrivial = constant.isTrivial(constantExpression, languageVersionSettings)
//                                if (isTrivial) {
//                                    // this constant is trivial -> report on first non trivial constant
//                                    val reportOn = notTrivialBranches.remove(constant)!!
//                                    report(reportOn)
//                                    checkedConstants[constant] = true
//                                } else {
//                                    // this constant is also not trivial -> report on it
//                                    report(constantExpression)
//                                }
//                            }
//                            null -> {
//                                // met constant for a first time
//                                val isTrivial = constant.isTrivial(constantExpression, languageVersionSettings)
//                                checkedConstants[constant] = isTrivial
//                                if (!isTrivial) {
//                                    notTrivialBranches[constant] = constantExpression
//                                }
//                            }
//                        }
//
//                    }
//                    is CjMatchConditionIsPattern -> {
//                        val typeReference = condition.typeReference ?: continue@conditions
//                        val type = trace.get(BindingContext.TYPE, typeReference) ?: continue@conditions
//                        val typeWithIsNegation = type to condition.isNegated
//                        if (checkedTypes.contains(typeWithIsNegation)) {
//                            trace.report(Errors.DUPLICATE_LABEL_IN_WHEN.on(typeReference))
//                        } else {
//                            checkedTypes.add(typeWithIsNegation)
//                        }
//                    }
//                    else -> {
//                    }
//                }
//            }
//        }
    }

    fun checkConnector(matchExpression: CjMatchExpression, trace: BindingTrace) {
        val bindingContext = trace.bindingContext

        //不能引入变量
        fun reportVariableIntroductionConflict(element: CjCasePattern) {
            trace.report(VARIABLE_INTRODUCTION_CONFLICT.on(element))
        }

        val connectorVisitor = object : CjVisitorVoid() {
            override fun visitPatternByType(element: CjTypePattern) {
                reportVariableIntroductionConflict(element)

            }

            override fun visitPatternByEnum(element: CjEnumPattern) {
                element.patterns.forEach {
                    it.accept(this)
                }
            }

            override fun visitPatternByTuple(element: CjTuplePattern) {
                element.patterns.forEach {
                    it.accept(this)
                }

            }

            override fun visitPatternByBinding(element: CjBindingPattern) {
                if (isBindingPattern(element, bindingContext)) {
                    reportVariableIntroductionConflict(element)
                }

            }
        }
//        val subjectExpression  = matchExpression.subjectExpression ?: return
//        val subjectType = matchSubjectType(matchExpression, bindingContext)

        for (matchEntry in matchExpression.entries) {
            if (matchEntry.conditions.size < 2) continue
//
//            if(!matchEntry.conditions.all { it::class == matchEntry.conditions::class }){
////                不为同一类型 （从简单角度）
//            }
//            val firstPattern = matchEntry.conditions.first()

//            when (firstPattern) {
//                is CjEnumPattern -> {
//
//                }
//
//                is CjBindingPattern -> {
//
//                }
//            }


            for (condition in matchEntry.conditions) {
                condition.accept(connectorVisitor)

            }
        }

    }

    fun checkLiteralPattern(
        matchExpression: CjMatchExpression,
        subjectType: CangJieType?,
        context: BindingContext
    ): Boolean {
        subjectType ?: return false
        return when {
            subjectType.isBoolean() -> {

                val booleanValues = matchExpression.entries.flatMap {
                    it.conditions.filter { condition ->
                        condition is CjConstantPattern && condition.expression is CjConstantExpression && condition.expression?.elementType == BOOLEAN_CONSTANT


                    }.map { pattern -> pattern.text }
                }
                booleanValues.contains("true") && booleanValues.contains("false")
            }

            subjectType.isUnit() -> {
                matchExpression.entries.any {
                    it.conditions.any { condition ->
                        condition is CjConstantPattern && condition.expression is CjConstantExpression && condition.expression?.elementType == UNIT_CONSTANT
                    }
                }
            }

            else -> false
        }

    }

}

internal abstract class MatchOnClassExhaustivenessChecker : MatchExhaustivenessChecker {

    private fun getReference(expression: CjExpression?): CjSimpleNameExpression? =
        when (expression) {
            is CjSimpleNameExpression -> expression
            is CjQualifiedExpression -> getReference(expression.selectorExpression)
            else -> null
        }

    val ClassDescriptor.enumEntriesConstructor: Set<ClassAndEnumConstructorDescriptor>
        get() {
            val enumEntryList = enumEntries
            val _enumEntryList = mutableListOf<ClassAndEnumConstructorDescriptor>()

            enumEntryList.forEach {
                it as EnumEntryDescriptor
                _enumEntryList.add(it.unsubstitutedPrimaryConstructor as ClassAndEnumConstructorDescriptor)

//                _enumEntryList.addAll(it.getEnumEntryConstructorDescriptors())
            }
            return _enumEntryList.toSet()
        }

    val ClassDescriptor.enumEntries: Set<ClassDescriptor>
        get() = DescriptorUtils.getAllDescriptors(this.unsubstitutedInnerClassesScope)
            .filter {
                DescriptorUtils.isEnumEntry(it)
            }
            .filterIsInstance<ClassDescriptor>()
            .toSet()


    protected val ClassDescriptor.deepSealedSubclasses: Set<ClassAndEnumConstructorDescriptor>
        get() = this.sealedSubclasses.flatMapTo(mutableSetOf()) {
            it.subclasses
        }

    private val ClassDescriptor.subclasses: Set<ClassAndEnumConstructorDescriptor>
        get() = when {
            this.modality == Modality.SEALED -> this.deepSealedSubclasses
            this.kind == ClassKind.ENUM -> this.enumEntries
            else -> setOf(this)
        }

    private val CjCasePattern.negated
        get() =/* (this as? CjMatchConditionIsPattern)?.isNegated ?:*/ false

    private fun CjCasePattern.isRelevant(checkedDescriptor: ClassAndEnumConstructorDescriptor) =
        this !is CjMatchConditionWithExpression ||
//                DescriptorUtils.isObject(checkedDescriptor) ||
                when (checkedDescriptor) {
                    is ClassDescriptor -> DescriptorUtils.isEnumEntry(checkedDescriptor)
                    is EnumEntryConstructorDescriptor -> true
                    else -> false
                }

    private fun CjCasePattern.getCheckedDescriptor(context: BindingContext): ClassAndEnumConstructorDescriptor? {
        return when (this) {
//            is CjMatchConditionIsPattern -> {
//                val checkedType = context.get(BindingContext.TYPE, typeReference) ?: return null
//                TypeUtils.getClassDescriptor(checkedType)
//            }
            is CjTuplePattern -> {
                null
            }
//            is CjTypePattern -> {
//                val reference = expression?.let { getReference(it) } ?: return null
//                context.get(REFERENCE_TARGET, reference) as? ClassDescriptor
//                null
//            }
            is CjMatchConditionWithExpression -> {
                val reference = expression?.let { getReference(it) } ?: return null
                context.get(REFERENCE_TARGET, reference) as? ClassDescriptor
            }

            is CjEnumPattern -> {
                val reference = expression?.let { getReference(it) } ?: return null
                context.get(REFERENCE_TARGET, reference).let {
                    when (it) {

                        is ClassDescriptor -> it
                        is FakeCallableDescriptorForObject -> it.classDescriptor
                        is EnumEntryConstructorDescriptor -> it
                        is ClassConstructorDescriptor -> {
                            var constructor = it
                            while (constructor != null) {
                                if (constructor is EnumEntryConstructorDescriptor) {
                                    break
                                }

                                constructor = constructor.original
                            }
                            constructor as EnumEntryConstructorDescriptor
                        }

                        else -> null
                    }
                }
            }

            is CjBindingPattern -> {
                val reference = expression?.let { getReference(it) } ?: return null
                context.get(REFERENCE_TARGET, reference).let {
                    when (it) {
                        is ClassDescriptor -> it
                        is FakeCallableDescriptorForObject -> it.classDescriptor
                        else -> {
                            null
                        }
                    }
                }
            }

            else -> {
                null
            }
        }
    }

    fun <E> Set<E>.containsOrEquals(element: E): Boolean {

        return this.contains(element) || this.any { it?.equals(element) == true }

    }


    //    检查元组模式 枚举模式是否使用绑定模式或通配符模式进行覆盖
    fun checkBindingPatternOrWildcardPattern(
        condition: CjCasePattern,
        context: BindingContext,
        types: List<CangJieType> = emptyList()
    ): Boolean {


        return when (condition) {

            is CjEnumAndTuplePattern
                -> {
                val pattens = condition.patterns

                if (pattens.all { it is CjBindingPattern || it is CjWildcardPattern }) {
//                    全部覆盖
                    return true
                }
                var result = true
                pattens.forEachIndexed { index, value ->
                    when (value) {
                        is CjTypePattern -> {

                            types.getOrNull(index)?.let {
                                if (!checkTypePattern(value, it, context)) {
                                    return false
                                }
                            }
                        }

                        is CjWildcardPattern,
                        is CjBindingPattern -> {
                        }

                        else -> return false
                    }


                }
                result
            }

            else -> true

        }
    }

    protected fun getMissingClassCasesByOther(
        matchExpression: CjMatchExpression,
        type: CangJieType,
        context: BindingContext
    ): List<MatchMissingCase> {

//        for (matchEntry in matchExpression.entries) {
//            for (condition in matchEntry.conditions) {
//
//
//                // Checks are important only for nested subclasses of the sealed class
//                // In additional, check without "is" is important only for objects
//                if (!checkBindingPatternOrWildcardPattern(
//                        condition,
//                        context,
//                        listOf(type)
//                    )
//                ) {
//                    continue
//                }
//                return emptyList()
//            }
//        }
        return listOf(MatchMissingCase.OtherCheckIsMissing())
    }

    protected fun getMissingClassCasesByTuple(
        matchExpression: CjMatchExpression,
        subclasses: Set<ClassAndEnumConstructorDescriptor>,
        context: BindingContext
    ): List<MatchMissingCase> {
        if (subclasses.isEmpty()) return listOf(MatchMissingCase.Unknown)

        for (matchEntry in matchExpression.entries) {
            for (condition in matchEntry.conditions) {

                val types = (subclasses.first() as TupleConstructor).types

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (!checkBindingPatternOrWildcardPattern(
                        condition,
                        context,
                        types
                    )
                ) {
                    continue
                }
                return emptyList()
            }
        }
        return subclasses
            .map(::createMatchMissingCaseForClassOrEnum)
    }

    /**
     * 检查单个pattern是否被覆盖
     */

    override fun isOverwrite(
        pattern: CjCasePattern,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?
    ): Boolean {
        return isOverwrite(pattern, context)
    }

    fun isOverwrite(
        pattern: CjCasePattern,
        context: BindingContext,
        subclasses: Set<ClassAndEnumConstructorDescriptor> = emptySet()
    ): Boolean {
        if (pattern is CjBindingPattern || pattern is CjWildcardPattern) {
            return true
        }


        val checkedDescriptor = pattern.getCheckedDescriptor(context)
        if (checkedDescriptor == null && pattern !is CjTuplePattern) {
            return false
        }
        val types = when (checkedDescriptor) {
            is EnumEntryConstructorDescriptor -> checkedDescriptor.getConstructorTypes()
            else -> emptyList()
        }

        if (subclasses.size > 1) return false
        // Checks are important only for nested subclasses of the sealed class
        // In additional, check without "is" is important only for objects
        return checkBindingPatternOrWildcardPattern(
            pattern,
            context,
            types
        ) || checkedDescriptor?.let { pattern.isRelevant(it) } != true
    }

    protected fun getMissingClassCases(
        matchExpression: CjMatchExpression,
        subclasses: Set<ClassAndEnumConstructorDescriptor>,
        context: BindingContext
    ): List<MatchMissingCase> {

        // when on empty enum / sealed is considered non-exhaustive, see test whenOnEmptySealed
        if (subclasses.isEmpty()) return listOf(MatchMissingCase.Unknown)

        val checkedDescriptors = linkedSetOf<ClassAndEnumConstructorDescriptor>()
        for (matchEntry in matchExpression.entries) {
            for (condition in matchEntry.conditions) {
                val negated = condition.negated
                val checkedDescriptor = condition.getCheckedDescriptor(context) ?: continue
                val checkedDescriptorSubclasses = when (checkedDescriptor) {
                    is ClassDescriptor -> checkedDescriptor.subclasses
                    is EnumEntryConstructorDescriptor -> setOf(checkedDescriptor)
                    else -> error("Unexpected class descriptor")
                }
                val types = when (checkedDescriptor) {
                    is EnumEntryConstructorDescriptor -> checkedDescriptor.getConstructorTypes()
                    else -> emptyList()
                }

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (!checkBindingPatternOrWildcardPattern(
                        condition,
                        context,
                        types
                    ) || (checkedDescriptorSubclasses.none { subclasses.containsOrEquals(it) } ||
                            !condition.isRelevant(checkedDescriptor))
                ) {
                    continue
                }
                if (negated) {
                    if (checkedDescriptors.containsAll(checkedDescriptorSubclasses)) return listOf()
                    checkedDescriptors.addAll(subclasses)
                    checkedDescriptors.removeAll(checkedDescriptorSubclasses)
                } else {
                    checkedDescriptors.addAll(checkedDescriptorSubclasses)
                }
            }
        }
        return subclasses.filterNot { checkedDescriptors.containsOrEquals(it) }
            .map(::createMatchMissingCaseForClassOrEnum)
//        return (subclasses - checkedDescriptors).map(::createMatchMissingCaseForClassOrEnum)
    }


    private fun createMatchMissingCaseForClassOrEnum(classDescriptor: ClassAndEnumConstructorDescriptor): MatchMissingCase {
        val classId = when (classDescriptor) {
            is ClassDescriptor -> DescriptorUtils.getClassIdForNonLocalClass(classDescriptor)
            is EnumEntryConstructorDescriptor -> ClassIdByConstructor(
                classDescriptor.constructedClass.classId!!,
                classDescriptor.getConstructorTypes()
            )

            is TupleConstructor -> ClassIdByConstructor(
                ClassId(FqName.topLevel(Name.identifier("Tuple")), Name.identifier("Tuple")),
                classDescriptor.types
            )

            else -> error("Unexpected class descriptor")
        }
        val kind = when (classDescriptor) {
            is ClassDescriptor -> classDescriptor.kind
            is EnumEntryConstructorDescriptor -> ClassKind.ENUM_ENTRY
            is TupleConstructor -> ClassKind.TUPLE
            else -> {
                error("Unexpected class descriptor")
            }
        }
        return if (kind == ClassKind.TUPLE) {
            MatchMissingCase.TupleCheckIsMissing(

                CallableId(classId.relativeClassName, classId.shortClassName)
            )
        } else if (kind != ClassKind.ENUM_ENTRY) {
            MatchMissingCase.IsTypeCheckIsMissing(
                classId = classId,
                isSingleton = kind.isSingleton
            )
        } else {
            val enumClassId = classId.outerClassId ?: error("Enum should have class id")
            MatchMissingCase.EnumCheckIsMissing(CallableId(enumClassId, classId.shortClassName))
        }
    }
}

private object MatchOnOtherExhaustivenessChecker : MatchOnClassExhaustivenessChecker() {

    override fun isApplicable(subjectType: CangJieType): Boolean {
        return MatchChecker.getClassDescriptorOfTypeIfTuple(subjectType) == null &&
                MatchChecker.getClassDescriptorOfTypeIfEnum(subjectType) == null
    }

    override fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        type: CangJieType?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        type ?: return emptyList()


        return buildList {
            addAll(
                getMissingClassCasesByOther(
                    expression,
                    type,
                    context
                )
            )

        }
    }

    override fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        return emptyList()
    }
}

private object MatchOnTupleExhaustivenessChecker : MatchOnClassExhaustivenessChecker() {
    override fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        type: CangJieType?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        type ?: return emptyList()
        assert(type.isBuiltinTupleType) { "isMatchOnEnumExhaustive should be called with an tuple class descriptor" }


        return buildList {
            addAll(
                getMissingClassCasesByTuple(
                    expression,
                    setOf(TupleConstructor(type.arguments.map { it.type })),
                    context
                )
            )

        }
    }

    override fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        return emptyList()
//        assert(isTuple(subjectDescriptor)) { "isMatchOnEnumExhaustive should be called with an tuple class descriptor" }
//        return buildList {
//            addAll(getMissingClassCases(expression, subjectDescriptor!!.enumEntriesConstructor, context))
//            addAll(MatchOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable))
//            addIfNotNull(MatchOnExpectExhaustivenessChecker.getMissingCase(subjectDescriptor))
//        }
    }

    override fun isApplicable(subjectType: CangJieType): Boolean {
        return MatchChecker.getClassDescriptorOfTypeIfTuple(subjectType) != null
    }
}

private object MatchOnEnumExhaustivenessChecker : MatchOnClassExhaustivenessChecker() {

    override fun isOverwrite(
        pattern: CjCasePattern,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,

        ): Boolean {

        return isOverwrite(pattern, context, subjectDescriptor?.enumEntriesConstructor ?: emptySet())
    }

    override fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<MatchMissingCase> {
        assert(isEnum(subjectDescriptor)) { "isMatchOnEnumExhaustive should be called with an enum class descriptor" }
        return buildList {
            addAll(getMissingClassCases(expression, subjectDescriptor!!.enumEntriesConstructor, context))
            addAll(MatchOnNullableExhaustivenessChecker.getMissingCases(expression, context, nullable))
            addIfNotNull(MatchOnExpectExhaustivenessChecker.getMissingCase(subjectDescriptor))
        }
    }

    override fun isApplicable(subjectType: CangJieType): Boolean {
        return MatchChecker.getClassDescriptorOfTypeIfEnum(subjectType) != null
    }
}


// It's not a regular exhaustiveness checker, invoke it only inside other checkers
private object MatchOnNullableExhaustivenessChecker /* : WhenExhaustivenessChecker*/ {
    fun getMissingCases(expression: CjMatchExpression, context: BindingContext, nullable: Boolean) =
        if (nullable) getNullCaseIfMissing(expression, context) else listOf()

    private fun getNullCaseIfMissing(expression: CjMatchExpression, context: BindingContext): List<MatchMissingCase> {
        for (entry in expression.entries) {
            for (condition in entry.conditions) {
                if (condition is CjMatchConditionWithExpression) {
                    condition.expression?.let {
                        val type = context.getType(it)
                        if (type != null && CangJieBuiltIns.isNullableNothing(type)) {
                            return listOf()
                        }
                    }
                }
            }
        }
        return listOf(MatchMissingCase.NullIsMissing)
    }
}

// It's not a regular exhaustiveness checker, invoke it only inside other checkers
private object MatchOnExpectExhaustivenessChecker {
    fun getMissingCase(subjectDescriptor: ClassDescriptor?): MatchMissingCase? {
        return runIf(subjectDescriptor?.isExpect == true) {
            when (subjectDescriptor!!.kind) {
                ClassKind.CLASS -> MatchMissingCase.ConditionTypeIsExpect.SealedClass
                ClassKind.INTERFACE -> MatchMissingCase.ConditionTypeIsExpect.SealedInterface
                ClassKind.ENUM -> MatchMissingCase.ConditionTypeIsExpect.Enum
                else -> MatchMissingCase.Unknown
            }
        }
    }
}

//    检查是否有type模式覆盖
fun checkTypePattern(condition: CjTypePattern, type: CangJieType?, context: BindingContext): Boolean {
    type ?: return false
    val typeReference = condition.typeReference

    val typeByPsi = typeReference?.getType(context) ?: return false

    return CangJieTypeChecker.DEFAULT.equalTypes(typeByPsi, type)


}

fun isBindingPattern(pattern: CjBindingPattern, context: BindingContext): Boolean {
    return context.get(
        VARIABLE,
        pattern
    ) != null
}


fun isOverwriteForForInExpr(pattern: CjCasePattern, expression: CjExpression?, trace: BindingTrace) {
    val isOverwrite = isOverwrite(pattern, expression, trace.bindingContext)
    if (!isOverwrite || pattern is CjTypePattern) {
        trace.report(IRREFUTABLE_PATTERN_FOR_IN_ERROR.on(pattern))
    }

}

fun isOverwriteForVariableDeclaration(pattern: CjCasePattern, expression: CjExpression?, trace: BindingTrace) {
    val isOverwrite = isOverwrite(pattern, expression, trace.bindingContext)
    if (!isOverwrite || pattern is CjTypePattern) {
        trace.report(IRREFUTABLE_PATTERN_ERROR.on(pattern))
    }

}

fun isOverwrite(pattern: CjCasePattern, expression: CjExpression?, context: BindingContext): Boolean {


    return pattern.getExhaustive(
        expression, context
    ) == null
}
