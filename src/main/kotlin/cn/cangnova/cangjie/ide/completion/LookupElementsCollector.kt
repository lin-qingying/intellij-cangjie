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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.MemberDescriptor
import cn.cangnova.cangjie.ide.intentions.CangJieInsertExplicitTypeArgumentsIntention
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjDotQualifiedExpression
import cn.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import cn.cangnova.cangjie.resolve.ImportedFromObjectCallableDescriptor
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
