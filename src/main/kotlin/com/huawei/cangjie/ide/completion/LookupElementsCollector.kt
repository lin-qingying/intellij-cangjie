package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.MemberDescriptor
import com.huawei.cangjie.ide.intentions.CangJieInsertExplicitTypeArgumentsIntention
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjDotQualifiedExpression
import com.huawei.cangjie.psi.psiUtil.collectDescendantsOfType
import com.huawei.cangjie.resolve.ImportedFromObjectCallableDescriptor
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.patterns.ElementPattern
import kotlin.math.max

class LookupElementsCollector(
    private val onFlush: () -> Unit,
    private val prefixMatcher: PrefixMatcher,
    private val completionParameters: CompletionParameters,
    resultSet: CompletionResultSet,
    sorter: CompletionSorter,
    private val filter: ((LookupElement) -> Boolean)?,
    private val allowExpectDeclarations: Boolean
) {
    var bestMatchingDegree = Int.MIN_VALUE
        private set
    private val elements = ArrayList<LookupElement>()
    val resultSet = resultSet.withPrefixMatcher(prefixMatcher).withRelevanceSorter(sorter)
    var isResultEmpty: Boolean = true
        private set

    fun flushToResultSet() {
        if (elements.isNotEmpty()) {
            onFlush()

            resultSet.addAllElements(elements)
            elements.clear()
            isResultEmpty = false
        }
    }

    private val postProcessors = ArrayList<(LookupElement) -> LookupElement>()
    private val processedCallables = HashSet<CallableDescriptor>()

    fun addDescriptorElements(
        descriptors: Iterable<DeclarationDescriptor>,
        lookupElementFactory: AbstractLookupElementFactory,
        notImported: Boolean = false,
        withReceiverCast: Boolean = false,
        prohibitDuplicates: Boolean = false
    ) {
        for (descriptor in descriptors) {
            addDescriptorElements(descriptor, lookupElementFactory, notImported, withReceiverCast, prohibitDuplicates)
        }
    }

    fun addDescriptorElements(
        descriptor: DeclarationDescriptor,
        lookupElementFactory: AbstractLookupElementFactory,
        notImported: Boolean = false,
        withReceiverCast: Boolean = false,
        prohibitDuplicates: Boolean = false
    ) {
        if (prohibitDuplicates && descriptor is CallableDescriptor && unwrapIfImportedFromObject(descriptor) in processedCallables) return

        var lookupElements =
            lookupElementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)

        if (withReceiverCast) {
            lookupElements = lookupElements.map { it.withReceiverCast() }
        }

        addElements(lookupElements, notImported)

        if (prohibitDuplicates && descriptor is CallableDescriptor) processedCallables.add(
            unwrapIfImportedFromObject(
                descriptor
            )
        )
    }

    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        postProcessors.add(processor)
    }

    fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>) {
        resultSet.restartCompletionOnPrefixChange(prefixCondition)
    }

    fun addElements(elements: Iterable<LookupElement>, notImported: Boolean = false) {
        elements.forEach { addElement(it, notImported) }
    }

    fun addElement(element: LookupElement, notImported: Boolean = false) {
        if (!prefixMatcher.prefixMatches(element)) {
            return
        }
        if (!allowExpectDeclarations) {
            val descriptor = (element.`object` as? DescriptorBasedDeclarationLookupObject)?.descriptor
            if ((descriptor as? MemberDescriptor)?.isExpect == true) return
        }

        if (notImported) {
            element.putUserData(NOT_IMPORTED_KEY, Unit)
            if (isResultEmpty && elements.isEmpty()) { /* without these checks we may get duplicated items */
                addElement(element.suppressAutoInsertion())
            } else {
                addElement(element)
            }
            return
        }

        val decorated = element
//            .let { HandleCompletionCharLookupElementDecorator(it, completionParameters) }
            .let { InsertExplicitTypeArgumentsLookupElementDecorator(it) }


        var result: LookupElement = decorated
        for (postProcessor in postProcessors) {
            result = postProcessor(result)
        }

        val declarationLookupObject = result.`object` as? DescriptorBasedDeclarationLookupObject
        if (declarationLookupObject != null) {
            result = DeclarationLookupObjectLookupElementDecorator(result, declarationLookupObject)
        }

        if (filter?.invoke(result) != false) {
            elements.add(result)
        }

        val matchingDegree = RealPrefixMatchingWeigher.getBestMatchingDegree(result, prefixMatcher)
        bestMatchingDegree = max(bestMatchingDegree, matchingDegree)
    }

}

/**
 * 声明补全
 */
private class DeclarationLookupObjectLookupElementDecorator(
    element: LookupElement,
    private val declarationLookupObject: DescriptorBasedDeclarationLookupObject
) : LookupElementDecorator<LookupElement>(element) {
    override fun getPsiElement() = declarationLookupObject.psiElement
}

private class InsertExplicitTypeArgumentsLookupElementDecorator(
    element: LookupElement,
) : LookupElementDecorator<LookupElement>(element) {
    override fun getDecoratorInsertHandler(): InsertHandler<LookupElementDecorator<LookupElement>> =
        InsertHandler { context, decorator ->
            delegate.handleInsert(context)

            val (typeArgs, exprOffset) = argList ?: return@InsertHandler
            val beforeCaret = context.file.findElementAt(exprOffset) ?: return@InsertHandler
            val callExpr = when (val beforeCaretExpr = beforeCaret.prevSibling) {
                is CjCallExpression -> beforeCaretExpr
                is CjDotQualifiedExpression -> beforeCaretExpr.collectDescendantsOfType<CjCallExpression>().lastOrNull()
                else -> null
            } ?: return@InsertHandler

            CangJieInsertExplicitTypeArgumentsIntention.applyTo(callExpr, typeArgs, true)
        }
}
private fun unwrapIfImportedFromObject(descriptor: CallableDescriptor): CallableDescriptor =
    if (descriptor is ImportedFromObjectCallableDescriptor<*>) descriptor.callableFromObject else descriptor
