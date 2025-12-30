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

package org.cangnova.cangjie.completion.smart

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.completion.*
import org.cangnova.cangjie.completion.handlers.WithTailInsertHandler
import org.cangnova.cangjie.completion.keywords.KeywordValues

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isAlmostEverything
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.toFuzzyType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.indices.ArgumentPositionData
import org.cangnova.cangjie.indices.COMPARISON_TOKENS
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.indices.ExpectedInfos
import org.cangnova.cangjie.indices.fuzzyType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.isBoolean
import org.cangnova.cangjie.types.makeNonOption

interface InheritanceItemsSearcher {
    fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit)
}
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
                        if (DescriptorUtils.isEnumConstructor(target)) {
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
    fun additionalItems(lookupElementFactory: LookupElementFactory): Pair<Collection<LookupElement>, InheritanceItemsSearcher?> {
        val (items, inheritanceSearcher) = additionalItemsNoPostProcess(lookupElementFactory)
        val postProcessedItems = items.map { postProcess(it) }

        val postProcessedSearcher = if (inheritanceSearcher != null)
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    inheritanceSearcher.search(nameFilter) { consumer(postProcess(it)) }
                }
            }
        else
            null
        return postProcessedItems to postProcessedSearcher
    }
    private fun postProcess(item: LookupElement): LookupElement {
        if (forBasicCompletion) return item
        return if (item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) == null) {
            LookupElementDecorator.withDelegateInsertHandler(
                item,
                InsertHandler { context, element ->
                    if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
                        val offset = context.offsetMap.tryGetOffset(OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                        if (offset != null) {
                            context.document.deleteString(context.tailOffset, offset)
                        }
                    }

                    element.handleInsert(context)
                }
            )
        } else {
            item
        }
    }
    private fun buildForAsTypePosition(lookupElementFactory: BasicLookupElementFactory): Collection<LookupElement>? {
        val binaryExpression =
            ((expression.parent as? CjUserType)?.parent as? CjTypeReference)?.parent as? CjBinaryExpressionWithTypeRHS ?: return null
        val elementType = binaryExpression.operationReference.referencedNameElementType
        if (elementType != CjTokens.AS_KEYWORD ) return null
        val expectedInfos = calcExpectedInfos(binaryExpression)

        val expectedInfosGrouped: Map<CangJieType?, List<ExpectedInfo>> = expectedInfos.groupBy { it.fuzzyType?.type?.makeNonOption() }

        val items = ArrayList<LookupElement>()
        for ((type, infos) in expectedInfosGrouped) {
            if (type == null) continue
            val lookupElement = lookupElementFactory.createLookupElementForType(type) ?: continue
            items.add(lookupElement.addTailAndNameSimilarity(infos))
        }
        return items
    }
    private fun MutableCollection<LookupElement>.addNamedArgumentsWithLiteralValueItems(expectedInfos: Collection<ExpectedInfo>) {
        data class NameAndValue(val name: Name, val value: String, val priority: SmartCompletionItemPriority)

        val nameAndValues = HashMap<NameAndValue, MutableList<ExpectedInfo>>()

        fun addNameAndValue(name: Name, value: String, priority: SmartCompletionItemPriority, expectedInfo: ExpectedInfo) {
            nameAndValues.getOrPut(NameAndValue(name, value, priority)) { ArrayList() }.add(expectedInfo)
        }

        for (expectedInfo in expectedInfos) {
            val argumentData = expectedInfo.additionalData as? ArgumentPositionData.Positional ?: continue
            if (argumentData.namedArgumentCandidates.isEmpty()) continue
            val parameters = argumentData.function.valueParameters
            if (argumentData.argumentIndex >= parameters.size) continue
            val parameterName = parameters[argumentData.argumentIndex].name

            if (expectedInfo.fuzzyType?.type?.isBoolean == true) {
                addNameAndValue(parameterName, "true", SmartCompletionItemPriority.NAMED_ARGUMENT_TRUE, expectedInfo)
                addNameAndValue(parameterName, "false", SmartCompletionItemPriority.NAMED_ARGUMENT_FALSE, expectedInfo)
            }
            if (expectedInfo.fuzzyType?.type?.isOption == true) {
                addNameAndValue(parameterName, "null", SmartCompletionItemPriority.NAMED_ARGUMENT_NULL, expectedInfo)
            }
        }

        for ((nameAndValue, infos) in nameAndValues) {
            var lookupElement = createNamedArgumentWithValueLookupElement(nameAndValue.name, nameAndValue.value, nameAndValue.priority)
            lookupElement = lookupElement.addTail(mergeTails(infos.map { it.tail }))
            add(lookupElement)
        }
    }
    private fun createNamedArgumentWithValueLookupElement(name: Name, value: String, priority: SmartCompletionItemPriority): LookupElement {
        val lookupElement = LookupElementBuilder.create("${name.asString()} = $value")
            .withIcon(CangJieIcons.PARAMETER)
            .withInsertHandler { context, _ ->
                context.document.replaceString(context.startOffset, context.tailOffset, "${name.render()} = $value")
            }
        lookupElement.putUserData(SmartCompletionInBasicWeigher.NAMED_ARGUMENT_KEY, Unit)
        lookupElement.assignSmartCompletionPriority(priority)
        return lookupElement
    }

    private fun additionalItemsNoPostProcess(lookupElementFactory: LookupElementFactory): Pair<Collection<LookupElement>, InheritanceItemsSearcher?> {
            val asTypePositionItems = buildForAsTypePosition(lookupElementFactory.basicFactory)
        if (asTypePositionItems != null) {
            assert(expectedInfos.isEmpty())
            return Pair(asTypePositionItems, null)
        }

        val items = ArrayList<LookupElement>()
        val inheritanceSearchers = ArrayList<InheritanceItemsSearcher>()

        if (!forBasicCompletion) { // basic completion adds keyword values on its own
            val keywordValueConsumer = object : KeywordValues.Consumer {
                override fun consume(
                    lookupString: String,
                    expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
                    suitableOnPsiLevel: PsiElement.() -> Boolean,
                    priority: SmartCompletionItemPriority,
                    factory: () -> LookupElement
                ) {
                    items.addLookupElements(null, expectedInfos, expectedInfoMatcher) {
                        val lookupElement = factory()
                        lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                        listOf(lookupElement)
                    }
                }
            }
            KeywordValues.process(
                keywordValueConsumer,
                expression,
                callTypeAndReceiver,
                bindingContext,
                resolutionFacade,
                moduleDescriptor,

            )
        }

        if (expectedInfos.isNotEmpty()) {
            items.addArrayLiteralsInAnnotationsCompletions()

            if (!forBasicCompletion && (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT || callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN /* after this@ */)) {
                items.addThisItems(expression, expectedInfos, smartCastCalculator)
            }

            if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                TypeInstantiationItems(
                    resolutionFacade,
                    bindingContext,
                    visibilityFilter,
                    toFromOriginalFileMapper,
                    inheritorSearchScope,
                    lookupElementFactory,
                    forBasicCompletion,
                    indicesHelper
                ).addTo(items, inheritanceSearchers, expectedInfos)

                if (expression is CjSimpleNameExpression) {
                    StaticMembers(bindingContext, lookupElementFactory, resolutionFacade, moduleDescriptor, applicabilityFilter)
                        .addToCollection(
                            items,
                            expectedInfos,
                            expression,
                            descriptorsToSkip
                        )
                }

                ClassLiteralItems.addToCollection(items, expectedInfos, lookupElementFactory.basicFactory )

                items.addNamedArgumentsWithLiteralValueItems(expectedInfos)

                LambdaSignatureItems.addToCollection(items, expressionWithType, bindingContext, resolutionFacade)

                if (!forBasicCompletion) {
                    LambdaItems.addToCollection(items, expectedInfos)

                    val matchCondition = expressionWithType.parent as? CjMatchConditionWithExpression
                    if (matchCondition != null) {
                        val entry = matchCondition.parent as CjMatchEntry
                        val whenExpression = entry.parent as CjMatchExpression
                        val entries = whenExpression.entries
                        if (whenExpression.elseExpression == null && entry == entries.last() && entries.size != 1) {
                            val lookupElement = LookupElementBuilder.create("case _ ")
                                .bold()
                                .withTailText(" =>")
                                .withInsertHandler(
                                    WithTailInsertHandler("=>", spaceBefore = true, spaceAfter = true).asPostInsertHandler
                                )

                            items.add(lookupElement)
                        }
                    }
                }

                MultipleArgumentsItemProvider(bindingContext, smartCastCalculator, resolutionFacade).addToCollection(
                    items,
                    expectedInfos,
                    expression
                )
            }
        }

        val inheritanceSearcher = if (inheritanceSearchers.isNotEmpty())
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    inheritanceSearchers.forEach { it.search(nameFilter, consumer) }
                }
            }
        else
            null

        return Pair(items, inheritanceSearcher)
    }

    private fun MutableCollection<LookupElement>.addThisItems(
        place: CjExpression,
        expectedInfos: Collection<ExpectedInfo>,
        smartCastCalculator: SmartCastCalculator
    ) {
        if (shouldCompleteThisItems(prefixMatcher)) {
            val items = thisExpressionItems(bindingContext, place, prefixMatcher.prefix, resolutionFacade)
            for (item in items) {
                val types = smartCastCalculator.types(item.receiverParameter).map { it.toFuzzyType(emptyList()) }
                val matcher = { expectedInfo: ExpectedInfo -> types.matchExpectedInfo(expectedInfo) }
                addLookupElements(null, expectedInfos, matcher) {
                    listOf(item.createLookupElement().assignSmartCompletionPriority(SmartCompletionItemPriority.THIS))
                }
            }
        }
    }
    private fun MutableCollection<LookupElement>.addArrayLiteralsInAnnotationsCompletions() {
        this.addAll(ArrayLiteralsInAnnotationItems.collect(expectedInfos, expression))
    }

    companion object{
        val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")

    }
}
