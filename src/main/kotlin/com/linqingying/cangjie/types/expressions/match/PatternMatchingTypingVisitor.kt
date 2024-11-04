package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.CjNodeTypes.BOOLEAN_CONSTANT
import com.linqingying.cangjie.CjNodeTypes.UNIT_CONSTANT
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames.ITERABLE
import com.linqingying.cangjie.builtins.isBuiltinTupleType
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.descriptors.enumd.LazyEnumDescriptor
import com.linqingying.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.linqingying.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.diagnostics.MatchMissingCase
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.*
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.BindingContext.*
import com.linqingying.cangjie.resolve.DescriptorUtils.isEnum
import com.linqingying.cangjie.resolve.caches.ConfusingMatchBranchSyntaxChecker
import com.linqingying.cangjie.resolve.caches.PrimitiveNumericComparisonCallChecker
import com.linqingying.cangjie.resolve.calls.checkers.RttiExpressionInformation
import com.linqingying.cangjie.resolve.calls.checkers.RttiOperation
import com.linqingying.cangjie.resolve.calls.context.ContextDependency
import com.linqingying.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.linqingying.cangjie.resolve.descriptorUtil.classId
import com.linqingying.cangjie.resolve.descriptorUtil.classValueType
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.*
import com.linqingying.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createCallForSpecialConstruction
import com.linqingying.cangjie.types.expressions.ControlStructureTypingUtils.Companion.createDataFlowInfoForArgumentsOfMatchCall
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.types.util.*
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.utils.addIfNotNull
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo
import com.linqingying.cangjie.utils.runIf
import com.intellij.psi.PsiElement
import java.util.*

abstract class Subject(
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

    class Type(typeInfo: CangJieTypeInfo, _dataFlowValue: DataFlowValue) : Subject(null, typeInfo, null) {


        init {

            dataFlowValue = _dataFlowValue

        }

        override fun createDataFlowValue(
            contextAfterSubject: ExpressionTypingContext,
            builtIns: CangJieBuiltIns
        ): DataFlowValue {
            return dataFlowValue
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

        expression.pattern?.let { checkCasePattern(subject, it, contextAfterSubject) }
        return noTypeInfo(context)
    }

    fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePattern,
        receiver: ReceiverValue,
        initializer: CjExpression,
        context: ExpressionTypingContext
    ) {
//        获取原始迭代器对象
        val iterator = receiver.type.extractSuperType(ITERABLE)
//        被迭代对象
        val iteratedType = iterator.arguments.first().type


        val subject =
            Subject.Expression(initializer, createTypeInfo(iteratedType), context.dataFlowValueFactory).apply {
                initDataFlowValue(context, components.builtIns)
            }

        checkCasePattern(
            subject, casePattern, context, Config(false)
        )
        initializer.findParentOfType<CjPatternEntryBlock>()?.let {
            context.config.addVariableDescriptor[it]?.let { variables ->
                variables.forEach { variable ->
                    variable(writableScope)
                }


            }
            context.config.addVariableDescriptor.remove(it)
        }
        val isOverwrite = MatchChecker.isOverwrite(casePattern, iteratedType, context.trace.bindingContext)
        if (!isOverwrite) {
            context.trace.report(IRREFUTABLE_PATTERN_ERROR.on(casePattern))
        }


    }

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


        if (subject is Subject.Expression) {
            checkExhaustive(subject, expression, contextAfterSubject)
        }

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

        val branchesTypeInfo =
            joinMatchExpressionBranches(
                expression,
                contextAfterSubject,
                matchReturnType,
                subject.jumpOutPossible,
                matchResultValue
            )

        val isExhaustive = MatchChecker.isMatchExhaustive(expression, trace)

        val branchesDataFlowInfo = branchesTypeInfo.dataFlowInfo
        val resultDataFlowInfo = if (expression.elseExpression == null && !isExhaustive) {
            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
            // because data flow can bypass all when branches in this case
            branchesDataFlowInfo.or(contextAfterSubject.dataFlowInfo)
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
    )

    private fun checkCasePattern(
        subject: Subject,
        condition: CjCasePattern,
        context: ExpressionTypingContext,
        config: Config = Config()
    ): ConditionalDataFlowInfo {

        var newDataFlowInfo = noChange(context)

        val patternVisitor = object : CjVisitorVoid() {

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

            //            枚举模式
            override fun visitPatternByEnum(element: CjEnumPattern) {
                context.config.getEnumEntryType = true


                val descriptors: List<EnumEntryDescriptor> = if (subject.type.isEnumEntry() || subject.type.isEnum()) {
                    subject.type.memberScope.getContributedClassifiers(
                        Name.identifier(element.expression?.text ?: ""),
                        NoLookupLocation.FROM_PACKAGE
                    ).mapNotNull {
                        if (it is LazySubstitutingClassDescriptor) {
                            it.original as? EnumEntryDescriptor
                        } else
                            it as? EnumEntryDescriptor
                    }

                } else {
                    listOf(
                        when (val it = element.expression?.let {
                            when (it) {
                                is CjQualifiedExpression -> components.callExpressionResolver.getQualifiedExpressionEnumEntryType(
                                    it,
                                    context
                                )

                                is CjSimpleNameExpression -> components.callExpressionResolver.getSimpleNameExpressionEnumEntryType(
                                    it,
                                    null,
                                    null,
                                    context
                                )

                                else -> {}
                            }
                        }) {
                            is EnumEntryDescriptor -> it

                            else -> {
                                context.trace.report(NOT_ENUM_ENTRY_VALUE.on(element.expression))
                                return
                            }
                        }
                    )
                }


                if (descriptors.isEmpty()) return


                val type = descriptors.first().defaultType
                if (!type.isEnumEntry()) {
                    context.trace.report(NOT_ENUM_ENTRY_VALUE.on(element.expression))
                    return
                }
                val enumType1 = subject.type
                val enumType2 = descriptors.first().classValueType ?: return
                if (!CangJieTypeChecker.DEFAULT.equalsIgnoringGenerics(enumType1, enumType2)) {
                    context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                    return
                }


//                检查参数数量
                val patterns = element.patterns
                val patternSize = patterns.size

                val constructor = descriptors.map { it.unsubstitutedPrimaryConstructor }.filter {
                    patternSize == it.valueParameters.size
                }
                if (constructor.isEmpty() && (patternSize != 0)) {
                    context.trace.report(ENUM_CONSTRUCTOR_MISMATCH.on(element.expression, patternSize))
                    return
                }
                constructor.forEach {
                    it as EnumEntryConstructorDescriptor

//                    TODO 将使用构建器的枚举项的引用替换为构造器
                    context.trace.record(REFERENCE_TARGET, element.expression!!.referenceExpression(), it)

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
                    element.findParentOfType<CjPatternEntryBlock>()?.let {
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


            fun ClassifierDescriptor?.findEnumEntryClassDescriptor(): EnumEntryDescriptor? {
                var descriptor: ClassifierDescriptor? = this
                while (descriptor != null) {
                    if (descriptor is EnumEntryDescriptor) {
                        break
                    }
                    descriptor = descriptor.original
                }
                return descriptor as? EnumEntryDescriptor
            }

            //            绑定模式  生成变量  TODO 与枚举模式混淆
            override fun visitPatternByBinding(element: CjBindingPattern) {

//                由于仓颉的枚举默认展开，所以会出现查找到多个枚举项的情况，而这种情况下条件值与模式值会出现不同一枚举类型下
//                如果条件值是枚举类型，则直接在条件值的枚举类型中查找
//                否则在scope中查找一个!枚举值

                val classDescriptor =
                    (if (subject.type.isEnumEntry() || subject.type.isEnum()) {
                        subject.type.memberScope.getContributedClassifier(
                            Name.identifier(element.text),
                            NoLookupLocation.FROM_PACKAGE
                        )
                    } else {
                        context.scope.findClassifier(Name.identifier(element.text), NoLookupLocation.FROM_PACKAGE)
                    } ?: context.scope.findClassifier(
                        Name.identifier(element.text),
                        NoLookupLocation.FROM_PACKAGE
                    )).findEnumEntryClassDescriptor()
//                context.scope.findClassifiers(Name.identifier(element.text), NoLookupLocation.FROM_PACKAGE)


//                if (  enumTypeInfo?.type ?.isEnum() == true) {
//             如具有无参构造器 使用枚举覆盖psi
                if (config.bindEnumType && classDescriptor != null && classDescriptor.kind == ClassKind.ENUM_ENTRY /*&& classDescriptor.hasUnsubstitutedPrimaryConstructor()*/) {
//                        优先使用enum枚举覆盖

                    if (!classDescriptor.hasUnsubstitutedPrimaryConstructor()) {
                        context.trace.report(NOT_ENUM_PARAMETER_CONSTRUCTOR.on(element))
                        return
                    }

                    val enumType1 = subject.type

                    val enumType2 = classDescriptor.classValueType

                    if (enumType2 != null && !CangJieTypeChecker.DEFAULT.equalsIgnoringGenerics(enumType1, enumType2)) {
                        context.trace.report(NOT_ENUM_MATCH.on(element.expression))

                    } else {
                        val enumTypeInfo = createTypeInfo(classDescriptor.defaultType)

//                        val enumTypeInfo = element.expression?.let { facade.getTypeInfo(it, context) }
                        context.trace.record(REFERENCE_TARGET, element.expression, classDescriptor)
                        context.trace.record(EXPRESSION_TYPE_INFO, subject.valueExpression, enumTypeInfo)
                    }
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

                    element.findParentOfType<CjPatternEntryBlock>()?.let {
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


                val patterns = element.patterns
                val patternSize = patterns.size

                if (!subject.type.isBuiltinTupleType) {
                    context.trace.report(TUPLE_PATTERN_TYPE_MISMATCH.on(element, subject.type))
                    return
                }

                if (patternSize < 1) {
                    context.trace.report(TUPLE_ARGS_TOO_FEW.on(element))
                    return
                }
                if (patternSize != subject.type.arguments.size) {
                    context.trace.report(TUPLE_ARGS_MISMATCH.on(element, subject.type.arguments.size, patternSize))
                    return
                }
                subject.type.arguments.forEachIndexed { index, argumentType ->
                    checkCasePattern(
                        Subject.Type(

                            createTypeInfo(argumentType.type, context),
                            subject.dataFlowValue
                        ),
                        patterns[index],
                        context
                    )

                }

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

//        condition.accept(patternVisitor)
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


    inner class PatternVisitor : CjVisitor<Pattern, PatternContext>() {
        override fun visitPatternByConstant(element: CjConstantPattern, data: PatternContext): Pattern {
            val expression = element.expression!!
            val newContext = data.context
            val typeInfo = facade.getTypeInfo(expression, newContext)
            checkTypeCompatibility(newContext, typeInfo.type!!, data.subject.type, expression, true)

            return Pattern(
                data.subject.type,
                PatternKind.Const(
                    newContext.trace[COMPILE_TIME_VALUE, expression]!!.toConstantValue(typeInfo.type)
                )
            )

        }

        override fun visitPatternByEnum(element: CjEnumPattern, data: PatternContext): Pattern {

            val typeReference = element.type
            val typeElement = typeReference?.typeElement
            val typeQualifier = typeReference?.typeElement?.qualifier
            if ((typeElement as? CjUserType)?.typeArgumentList != null && typeQualifier != null) {
//               不能在没有前置类型的情况下使用类型参数
            }
            if (typeElement !is CjUserType) {
//                不是Cjuser一定不是枚举类型

            }

            val enumEntrys: List<EnumEntryDescriptor> = if (data.subject.type.isEnum() && typeQualifier == null) {
//            枚举具有重载性
                data.subject.type.memberScope.getContributedDescriptors {
                    it.asString() == typeElement?.text
                }.mapNotNull {
                    if (it is LazySubstitutingClassDescriptor) {
                        it.original as? EnumEntryDescriptor
                    } else
                        it as? EnumEntryDescriptor
                }.filter { !it.hasUnsubstitutedPrimaryConstructor() }
            } else {

                val newContext = data.context.replaceExpectedType(data.subject.type)
                val enumEntryDescriptor = element.type?.let {
                    (it.typeElement as? CjUserType)?.let { it1 ->
                        components.typeResolver.resolveClass(
                            newContext.scope,
                            it1,
                            newContext.trace,
                            false
                        )
                    }
                }
                if (enumEntryDescriptor == null) {
                    data.context.trace.report(NOT_ENUM_MATCH.on(element.type))
                    return Pattern(data.subject.type, PatternKind.Error)

                }
//                if (type is EnumEntryDescriptor) {
//                    data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
//                    data.context.trace.record(REFERENCE_TARGET, element.expression?.referenceExpression(), type)
//
//                    return Pattern(data.subject.type, PatternKind.Error)
//                }
                val entryByEnumType = (enumEntryDescriptor.containingDeclaration as? LazyEnumDescriptor)?.defaultType
                if (entryByEnumType == null) {
                    data.context.trace.report(NOT_ENUM_MATCH.on(element.type))
                    return Pattern(data.subject.type, PatternKind.Error)
                }
                if (!CangJieTypeChecker.DEFAULT.equalsIgnoringGenerics(data.subject.type, entryByEnumType)) {
                    data.context.trace.report(NOT_ENUM_MATCH.on(element.type))
                    return Pattern(data.subject.type, PatternKind.Error)
                }
//                检查类型参数
                if (typeQualifier?.typeArgumentsAsTypes?.isNotEmpty() == true) {
                    if (typeQualifier.typeArgumentsAsTypes.size != entryByEnumType.arguments.size) {
                        data.context.trace.report(NOT_ENUM_MATCH.on(element.type))
                        return Pattern(data.subject.type, PatternKind.Error)
                    }
                    data.subject.type.arguments.forEachIndexed { index, typeProjection ->
                       val tpType = components.typeResolver.resolveType(
                            data.context.scope,
                            typeQualifier.typeArgumentsAsTypes[index],
                            data.context.trace,
                            false
                        )
                        if(!CangJieTypeChecker.DEFAULT.isSubtypeOf(typeProjection.type, tpType)){
                            data.context.trace.report(NOT_ENUM_MATCH.on(element.type))
                            return Pattern(data.subject.type, PatternKind.Error)
                        }
                    }
                }

                emptyList()
            }
//            val type = element.type?.let {
//                components.typeResolver.resolveType(
//                    data.context.scope,
//                    it,
//                    data.context.trace,
//                    false
//                )
//            }
//            if (type?.isEnumEntry() == false) {
//                data.context.trace.report(NOT_ENUM_MATCH.on(element))
//                return Pattern(data.subject.type, PatternKind.Error)
//            }
//            val entryByEnumType = type?.constructor?.declarationDescriptor

            return super.visitPatternByEnum(element, data)
        }

        override fun visitPatternByBinding(element: CjBindingPattern, data: PatternContext): Pattern {
            val enumEntrys: List<EnumEntryDescriptor> = if (data.subject.type.isEnum()) {
//            枚举具有重载性
                data.subject.type.memberScope.getContributedDescriptors {
                    it.asString() == element.expression?.text
                }.mapNotNull {
                    if (it is LazySubstitutingClassDescriptor) {
                        it.original as? EnumEntryDescriptor
                    } else
                        it as? EnumEntryDescriptor
                }
            } else {
                val type =
                    data.context.scope.findClassifier(Name.identifier(element.text), NoLookupLocation.FROM_PACKAGE)
                if (type is EnumEntryDescriptor) {
                    data.context.trace.report(NOT_ENUM_MATCH.on(element.expression))
                    data.context.trace.record(REFERENCE_TARGET, element.expression, type)

                    return Pattern(data.subject.type, PatternKind.Error)
                }

                emptyList()
            }
            if (enumEntrys.isNotEmpty()) {
                enumEntrys.firstOrNull { it.hasUnsubstitutedPrimaryConstructor() }?.let {
                    data.context.trace.record(REFERENCE_TARGET, element.expression, it)

                    return Pattern(
                        data.subject.type,
                        PatternKind.Enum(
                            it.classLikeInfo.elementByE.getStrictParentOfType<CjEnum>()!!,
                            it.classLikeInfo.elementByE,
                            emptyList()
                        )
                    )
                }

                data.context.trace.report(NOT_ENUM_PARAMETER_CONSTRUCTOR.on(element))

                return Pattern(
                    data.subject.type,
                    PatternKind.Enum(
                        enumEntrys.first().classLikeInfo.elementByE.getStrictParentOfType<CjEnum>()!!,
                        enumEntrys.first().classLikeInfo.elementByE,
                        emptyList()
                    )
                )


            }


            return Pattern(data.subject.type, PatternKind.Binding(data.subject.type, element.text))
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


                    }.map { it.text }
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
                    else -> emptyList<CangJieType>()
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
