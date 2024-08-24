package com.huawei.cangjie.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement

class LookupElementsCollector(
    private val onFlush: () -> Unit,
    private val prefixMatcher: PrefixMatcher,
    private val completionParameters: CompletionParameters,
    resultSet: CompletionResultSet,
    sorter: CompletionSorter,
    private val filter: ((LookupElement) -> Boolean)?,
//    private val allowExpectDeclarations: Boolean
)
{
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

    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        postProcessors.add(processor)
    }

}
