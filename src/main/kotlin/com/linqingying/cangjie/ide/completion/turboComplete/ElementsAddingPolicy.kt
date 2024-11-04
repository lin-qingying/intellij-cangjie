package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionResultSet

import com.intellij.codeInsight.lookup.LookupElement

interface ElementsAddingPolicy {
    /**
     * Called when the policy should come into effect
     *
     * @see #onDeactivate
     */
    fun onActivate(result: CompletionResultSet)

    /**
     * Called when result's {@link com.intellij.codeInsight.completion.CompletionResultSet#stopHere} was called
     */
    fun onResultStop(result: CompletionResultSet)

    /**
     * Called when another [element] should be added to [result]
     */
    fun addElement(result: CompletionResultSet, element: LookupElement)

    /**
     * Called when all [elements] should be added to [result]
     */
    fun addAllElements(result: CompletionResultSet, elements: Iterable<LookupElement>)

    /**
     * Called when the policy should end its action
     *
     * @see #onActivate
     */
    fun onDeactivate(result: CompletionResultSet)

    interface Default : ElementsAddingPolicy {
        override fun onActivate(result: CompletionResultSet) {}

        override fun onResultStop(result: CompletionResultSet) {}

        override fun addElement(result: CompletionResultSet, element: LookupElement) = addAllElements(result, listOf(element))

        override fun addAllElements(result: CompletionResultSet, elements: Iterable<LookupElement>) = result.addAllElements(elements)

        override fun onDeactivate(result: CompletionResultSet) {}
    }
}
