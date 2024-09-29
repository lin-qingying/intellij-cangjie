package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.diagnostics.MatchMissingCase
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.findParentOfType
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContext.SMARTCAST
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.TypeResolutionContext
import com.huawei.cangjie.resolve.caches.PrimitiveNumericComparisonCallChecker
import com.huawei.cangjie.resolve.calls.checkers.RttiExpressionInformation
import com.huawei.cangjie.resolve.calls.checkers.RttiOperation
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.lazy.descriptors.LazyEnumEntryDescriptor
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import com.huawei.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfMatchCall
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.containsError
import com.huawei.cangjie.types.util.isEnumEntry
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo
import com.intellij.psi.PsiElement
import java.util.*

class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) :
    ExpressionTypingVisitor(facade) {


    inner class CasePatten

    override fun visitMatchExpression(
        expression: CjMatchExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo =
        visitMatchExpression(expression, context, false)

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

        val contextAfterSubject = run {
            var result = contextBeforeSubject
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }
        val contextWithExpectedTypeAndSubjectVariable =
            subject.scopeWithSubject?.let { contextWithExpectedType.replaceScope(it) } ?: contextWithExpectedType
        subject.initDataFlowValue(contextAfterSubject, components.builtIns)
        val possibleTypesForSubject =
            subject.typeInfo?.dataFlowInfo?.getStableTypes(subject.dataFlowValue, components.languageVersionSettings)
                ?: emptySet()

//        checkSmartCastsInSubjectIfRequired(expression, contextBeforeSubject, subject.type, possibleTypesForSubject)


        val dataFlowInfoForEntries = analyzeConditionsInMatchEntries(expression, contextAfterSubject, subject)
        val matchReturnType = inferTypeForMatchExpression(
            expression,
            subject,
            contextWithExpectedTypeAndSubjectVariable,
            contextAfterSubject,
            dataFlowInfoForEntries
        )
        val matchResultValue =
            matchReturnType?.let {
                facade.components.dataFlowValueFactory.createDataFlowValue(
                    expression,
                    it,
                    contextAfterSubject
                )
            }

//        val branchesTypeInfo =
//            joinMatchExpressionBranches(expression, contextAfterSubject, matchReturnType, subject.jumpOutPossible, matchResultValue)
//
//        val isExhaustive = MatchChecker.isMatchExhaustive(expression, trace)
//
//        val branchesDataFlowInfo = branchesTypeInfo.dataFlowInfo
//        val resultDataFlowInfo = if (expression.elseExpression == null && !isExhaustive) {
//            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
//            // because data flow can bypass all when branches in this case
//            branchesDataFlowInfo.or(contextAfterSubject.dataFlowInfo)
//        } else {
//            branchesDataFlowInfo
//        }
//
//        if (matchReturnType != null && isExhaustive && expression.elseExpression == null && KotlinBuiltIns.isNothing(whenReturnType)) {
//            trace.record(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, expression)
//        }
//
//        val branchesType = branchesTypeInfo.type ?: return noTypeInfo(resultDataFlowInfo)
//        val resultType = components.dataFlowAnalyzer.checkType(branchesType, expression, contextWithExpectedType)
//
//        ConfusingMatchBranchSyntaxChecker.check(expression, contextWithExpectedType.languageVersionSettings, trace)
//
//        return createTypeInfo(resultType, resultDataFlowInfo, branchesTypeInfo.jumpOutPossible, contextWithExpectedType.dataFlowInfo)

        return createTypeInfo(null)
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

    private fun checkCasePattern(
        subject: Subject,
        condition: CjCasePattern,
        context: ExpressionTypingContext
    ): ConditionalDataFlowInfo {

        var newDataFlowInfo = noChange(context)

        val patternVisitor = object : CjVisitorVoid() {
            //            枚举模式
            override fun visitPatternByEnum(element: CjEnumPattern) {
                context.config.getEnumEntryType = true

                val descriptor: ClassDescriptor = when (val it = element.expression?.let {
                    components.callExpressionResolver.getSimpleNameExpressionEnumEntryType(it, null, null, context)
                }) {
                    is FakeCallableDescriptorForObject -> it.classDescriptor

                    is ClassDescriptor -> it


                    else -> {
//                        context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                        context.trace.report(NOT_ENUM_ENTRY_VALUE.on(element.expression))

                        return
                    }
                }


                val type = descriptor.defaultType
                if (!type.isEnumEntry()) {
                    context.trace.report(NOT_ENUM_ENTRY_VALUE.on(element.expression))
                    return
                }

                val enumType1 = subject.type

                val enumType2 = descriptor.classValueType ?: return

                if (!CangJieTypeChecker.DEFAULT.equalTypes(enumType1, enumType2)) {
                    context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                    return
                }

//                检查参数数量
                val patterns = element.patterns
                val patternSize = patterns.size

                val constructor = descriptor.constructors.filter {
                    patternSize == it.valueParameters.size
                }
                if (constructor.isEmpty()) {
                    context.trace.report(ENUM_CONSTRUCTOR_MISMATCH.on(element.expression, patternSize))
                    return
                }


                constructor.forEach {
                    it as EnumEntryConstructorDescriptor

                    it.valueParameters.forEachIndexed { index, value ->


                        checkCasePattern(
                            Subject.Type(

                                createTypeInfo(value.type, context),
                                subject.dataFlowValue
                            ),
                            patterns[index],
                            context
                        )
                    }
                }
//                CangJieTypeChecker.DEFAULT.isSubtypeOf(type!!, subject.type)
                type
            }


            //            类型模式
            override fun visitPatternByType(element: CjTypePattern) {
                val type =
                    element.typeReference?.let {
                        components.typeResolver.resolveType(
                            context.scope,
                            it, context.trace, false
                        )
                    }

                val redeclarationChecker =
                    TraceBasedLocalRedeclarationChecker(
                        context.trace,
                        this@PatternMatchingTypingVisitor.components.overloadChecker
                    )
                val scope = LexicalWritableScope(
                    context.scope, context.scope.ownerDescriptor, false, redeclarationChecker,
                    LexicalScopeKind.CODE_BLOCK
                )
                val variable = components.localVariableResolver.resolveLocalVariableDescriptorWithType(
                    scope, element, type, context.trace
                )
//                if (context.scope is LexicalWritableScope) {
//                    (context.scope as LexicalWritableScope).addVariableDescriptor(variable)
//                }
                element.parent?.let {
                    element.findParentOfType<CjMatchEntry>()?.let {
                        if (context.config.addVariableDescriptor[it] == null) {
                            context.config.addVariableDescriptor[it] = mutableListOf()
                        }
                        context.config.addVariableDescriptor[it]?.add { scope ->
                            scope as LexicalWritableScope
                            scope.addVariableDescriptor(variable)
                        }

                    }

                }
            }

            //            绑定模式  生成变量  TODO 与枚举模式混淆
            override fun visitPatternByBinding(element: CjBindingPattern) {


//
                val classDescriptor =
                    context.scope.findClassifier(Name.identifier(element.text), NoLookupLocation.FROM_PACKAGE)


//                if (  enumTypeInfo?.type ?.isEnum() == true) {
//             如具有无参构造器 使用枚举覆盖psi
                if (classDescriptor != null && classDescriptor is LazyEnumEntryDescriptor && classDescriptor.hasUnsubstitutedPrimaryConstructor()) {
//                        优先使用enum枚举覆盖

                    val enumTypeInfo = element.expression?.let { facade.getTypeInfo(it, context) }

                } else {
                    val redeclarationChecker =
                        TraceBasedLocalRedeclarationChecker(
                            context.trace,
                            this@PatternMatchingTypingVisitor.components.overloadChecker
                        )
                    val scope = LexicalWritableScope(
                        context.scope, context.scope.ownerDescriptor, false, redeclarationChecker,
                        LexicalScopeKind.CODE_BLOCK
                    )
                    val variable = components.localVariableResolver.resolveLocalVariableDescriptorWithType(
                        scope, element, subject.type, context.trace
                    )

                    element.findParentOfType<CjMatchEntry>()?.let {
                        if (context.config.addVariableDescriptor[it] == null) {
                            context.config.addVariableDescriptor[it] = mutableListOf()
                        }
                        context.config.addVariableDescriptor[it]?.add { scope ->
                            scope as LexicalWritableScope
                            scope.addVariableDescriptor(variable)
                        }

                    }
                }
            }
//元组模式
//            需要sub expr类型为 tupleN (需类型一致）
//            每个元组项的类型需一致
//            元组个数需要大于1
            override fun visitPatternByTuple(element: CjTuplePattern) {

            }

            override fun visitPatternByConstant(element: CjConstantPattern) {

//                常量模式，当作表达式检查

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

        }

        condition.accept(patternVisitor)
        return newDataFlowInfo

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
        var contextForCondition = context
        for (condition in matchEntry.conditions) {
            val conditionInfo = checkCasePattern(subject, condition, contextForCondition)
            entryInfo = entryInfo?.let {
                ConditionalDataFlowInfo(it.thenInfo.or(conditionInfo.thenInfo), it.elseInfo.and(conditionInfo.elseInfo))
            } ?: conditionInfo

            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfo.elseInfo)
        }

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
        }
        return argumentDataFlowInfos
    }

    //    类型只能转换
    private fun checkSmartCastsInSubjectIfRequired(
        expression: CjMatchExpression,
        contextBeforeSubject: ExpressionTypingContext,
        subjectType: CangJieType,
        possibleTypesForSubject: Set<CangJieType>
    ) {

    }

    private abstract class Subject(
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

        class Type(typeInfo: CangJieTypeInfo, val _dataFlowValue: DataFlowValue) : Subject(null, typeInfo, null) {

            init {
                dataFlowValue = _dataFlowValue
            }

            override fun createDataFlowValue(
                contextAfterSubject: ExpressionTypingContext,
                builtIns: CangJieBuiltIns
            ): DataFlowValue {
                return _dataFlowValue
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

        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, CangJieTypeChecker.DEFAULT)) {
            context.trace.report(CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType))
        }
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
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo)
        }

//        expression.reportDeprecatedDefinitelyNotNullSyntax(expression.typeReference, contextWithExpectedType)

        val resultTypeInfo = components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(components.builtIns.boolType),
            expression,
            contextWithExpectedType
        )

        if (typeReference != null) {
            val rhsType = context.trace[BindingContext.TYPE, typeReference]
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
}

private interface MatchExhaustivenessChecker {
    fun getMissingCases(
        expression: CjMatchExpression,
        context: BindingContext,
        subjectDescriptor: ClassDescriptor?,
        nullable: Boolean
    ): List<MatchMissingCase>

    fun isApplicable(subjectType: CangJieType): Boolean = false
}

object MatchChecker {
    @JvmStatic
    fun getClassDescriptorOfTypeIfSealed(type: CangJieType?): ClassDescriptor? =
        type?.let { TypeUtils.getClassDescriptor(it) }?.takeIf { DescriptorUtils.isSealedClass(it) }

    @JvmStatic
    fun getClassDescriptorOfTypeIfEnum(type: CangJieType?): ClassDescriptor? {
        if (type == null) return null
        val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return null
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
//        MatchOnEnumExhaustivenessChecker,
//        MatchOnSealedExhaustivenessChecker
    )

    fun getMissingCases(expression: CjMatchExpression, context: BindingContext): List<MatchMissingCase> {
        val type = MatchSubjectType(expression, context) ?: return listOf(MatchMissingCase.Unknown)
        val nullable = type.isMarkedOption
        val checkers = exhaustivenessCheckers.filter { it.isApplicable(type) }
        if (checkers.isEmpty()) return listOf(MatchMissingCase.Unknown)
        return checkers.map { it.getMissingCases(expression, context, TypeUtils.getClassDescriptor(type), nullable) }
            .flatten()
    }


    @JvmStatic
    fun MatchSubjectType(expression: CjMatchExpression, context: BindingContext): CangJieType? {
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

}
