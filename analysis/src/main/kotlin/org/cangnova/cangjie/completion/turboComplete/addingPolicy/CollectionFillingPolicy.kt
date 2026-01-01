package org.cangnova.cangjie.completion.turboComplete.addingPolicy

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import org.cangnova.cangjie.completion.turboComplete.ElementsAddingPolicy

/**
 * Fill the given collection, when an element is added
 */

class CollectionFillingPolicy(private val addedElements: MutableCollection<LookupElement>) :
    ElementsAddingPolicy.Default {

    override fun addElement(result: CompletionResultSet, element: LookupElement) {
        addedElements.add(element)
    }

    override fun addAllElements(result: CompletionResultSet, elements: Iterable<LookupElement>) {
        addedElements.addAll(elements)
    }
}