package cn.cangnova.cangjie.ide.completion.addingPolicy

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import cn.cangnova.cangjie.ide.completion.turboComplete.ElementsAddingPolicy


/**
 * Pass all elements directly to the result set
 */
class PassDirectlyPolicy : ElementsAddingPolicy.Default {
    override fun addElement(result: CompletionResultSet, element: LookupElement) {
        result.addElement(element)
    }

    override fun addAllElements(result: CompletionResultSet, elements: Iterable<LookupElement>) {
        result.addAllElements(elements)
    }
}