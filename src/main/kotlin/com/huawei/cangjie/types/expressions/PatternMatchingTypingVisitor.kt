package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.TypeResolutionContext
import com.huawei.cangjie.resolve.calls.checkers.RttiExpressionInformation
import com.huawei.cangjie.resolve.calls.checkers.RttiOperation
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.smartcasts.ConditionalDataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.containsError
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) :
    ExpressionTypingVisitor(facade) {

    override fun visitMatchExpression(expression: CjMatchExpression, context: ExpressionTypingContext ): CangJieTypeInfo =
        visitMatchExpression(expression, context, false)

    fun visitMatchExpression(
        expression: CjMatchExpression,
        contextWithExpectedType: ExpressionTypingContext,
        @Suppress("UNUSED_PARAMETER") isStatement: Boolean
    ): CangJieTypeInfo {


        TODO()
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

    /*
       * (a: SubjectType) is Type
       */
    private fun checkTypeCompatibility(
        context: ExpressionTypingContext,
        type: CangJieType,
        subjectType: CangJieType,
        reportErrorOn: CjElement
    ): Boolean {
        // TODO : Take smart casts into account?
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
//            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType))
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
//fun CjOperationExpression.reportDeprecatedDefinitelyNotNullSyntax(
//    rhs: CjTypeReference?,
//    context: ExpressionTypingContext
//) {
//    val nextLeaf = nextLeaf()
//    if (nextLeaf is LeafPsiElement && nextLeaf.elementType === CjTokens.EXCLEXCL && rhs?.typeElement is CjUserType) {
//        val parent = PsiTreeUtil.findCommonParent(nextLeaf, this)
//        if (parent is CjPostfixExpression && parent.operationToken === CjTokens.EXCLEXCL) {
//            context.trace.report(Errors.DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL.on((parent as CjPostfixExpression?)!!))
//        }
//    }
//}
