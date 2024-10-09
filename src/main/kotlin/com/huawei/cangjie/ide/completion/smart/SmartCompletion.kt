package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.*
import com.huawei.cangjie.ide.completion.SmartCastCalculator
import com.huawei.cangjie.ide.completion.ToFromOriginalFileMapper
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.types.isAlmostEverything
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.psi.search.GlobalSearchScope

class SmartCompletion(
    private val expression: CjExpression,
    private val resolutionFacade: ResolutionFacade,
    private val bindingContext: BindingContext,
    private val moduleDescriptor: ModuleDescriptor,
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    private val applicabilityFilter: (DeclarationDescriptor) -> Boolean,
    private val indicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val inheritorSearchScope: GlobalSearchScope,
    private val toFromOriginalFileMapper: ToFromOriginalFileMapper,
    private val callTypeAndReceiver: CallTypeAndReceiver<*, *>,

    private val forBasicCompletion: Boolean = false
)
{
    private val expressionWithType = when (callTypeAndReceiver) {
        is CallTypeAndReceiver.DEFAULT ->
            expression

        is CallTypeAndReceiver.DOT,
        is CallTypeAndReceiver.SAFE,
        is CallTypeAndReceiver.SUPER_MEMBERS,

        is CallTypeAndReceiver.CALLABLE_REFERENCE ->
            expression.parent as CjExpression

        else -> // actually no smart completion for such places
            expression
    }
    val expectedInfos: Collection<ExpectedInfo> = calcExpectedInfos(expressionWithType)



    private fun implicitlyTypedDeclarationFromInitializer(expression: CjExpression): CjDeclaration? {
        when (val parent = expression.parent) {
            is CjVariableDeclaration -> if (expression == parent.initializer && parent.typeReference == null) return parent
            is CjNamedFunction -> if (expression == parent.initializer && parent.typeReference == null) return parent
        }
        return null
    }
    val descriptorsToSkip: Set<DeclarationDescriptor> by lazy {
        when (val parent = expressionWithType.parent) {
            is CjBinaryExpression -> {
                if (parent.right == expressionWithType) {
                    val operationToken = parent.operationToken
                    if (operationToken == CjTokens.EQ || operationToken in COMPARISON_TOKENS) {
                        val left = parent.left
                        if (left is CjReferenceExpression) {
                            return@lazy bindingContext[BindingContext.REFERENCE_TARGET, left]?.let(::setOf).orEmpty()
                        }
                    }
                }
            }

            is CjMatchConditionWithExpression -> {
                val entry = parent.parent as CjMatchEntry
                val whenExpression = entry.parent as CjMatchExpression
                val subject = whenExpression.subjectExpression ?: return@lazy emptySet()

                val descriptorsToSkip = HashSet<DeclarationDescriptor>()

                if (subject is CjSimpleNameExpression) {
                    val variable = bindingContext[BindingContext.REFERENCE_TARGET, subject] as? VariableDescriptor
                    if (variable != null) {
                        descriptorsToSkip.add(variable)
                    }
                }

                val subjectType = bindingContext.getType(subject) ?: return@lazy emptySet()
                val classDescriptor = TypeUtils.getClassDescriptor(subjectType)
                if (classDescriptor != null && DescriptorUtils.isEnum (classDescriptor)) {
                    val conditions =
                        whenExpression.entries.flatMap { it.conditions.toList() }.filterIsInstance<CjMatchConditionWithExpression>()
                    for (condition in conditions) {
                        val selectorExpr =
                            (condition.expression as? CjDotQualifiedExpression)?.selectorExpression as? CjReferenceExpression ?: continue
                        val target = bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr] as? ClassDescriptor
                            ?: continue
                        if (DescriptorUtils.isEnumEntry(target)) {
                            descriptorsToSkip.add(target)
                        }
                    }
                }

                return@lazy descriptorsToSkip
            }
        }
        return@lazy emptySet()
    }
    private fun calcExpectedInfos(expression: CjExpression): Collection<ExpectedInfo> {
        // if our expression is initializer of implicitly typed variable - take type of variable from original file (+ the same for function)
        val declaration = implicitlyTypedDeclarationFromInitializer(expression)
        if (declaration != null) {
            val originalDeclaration = toFromOriginalFileMapper.toOriginalFile(declaration)
            if (originalDeclaration != null) {
                val originalDescriptor = originalDeclaration.resolveToDescriptorIfAny() as? CallableDescriptor
                val returnType = originalDescriptor?.returnType
                if (returnType != null && !returnType.isError) {
                    return listOf(ExpectedInfo(returnType, declaration.name, null))
                }
            }
        }

        // if expected types are too general, try to use expected type from outer calls
        var count = 0
        while (true) {
            val infos =
                ExpectedInfos(bindingContext, resolutionFacade, indicesHelper, useOuterCallsExpectedTypeCount = count).calculate(expression)
            if (count == 2 /* use two outer calls maximum */ || infos.none { it.fuzzyType?.isAlmostEverything() == true }) {
                return if (forBasicCompletion)
                    infos.map { it.copy(tail = null) }
                else
                    infos
            }
            count++
        }
        //TODO: we could always give higher priority to results with outer call expected type used
    }
    val smartCastCalculator: SmartCastCalculator by lazy(LazyThreadSafetyMode.NONE) {
        SmartCastCalculator(
            bindingContext,
            resolutionFacade.moduleDescriptor,
            expression,
            callTypeAndReceiver.receiver as? CjExpression,
            resolutionFacade
        )
    }
    companion object{
        val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")

    }
}
