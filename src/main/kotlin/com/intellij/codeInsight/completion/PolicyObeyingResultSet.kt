package com.intellij.codeInsight.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.NlsContexts
import com.intellij.patterns.ElementPattern
import com.linqingying.cangjie.ide.completion.turboComplete.ElementsAddingPolicy
//
//class PolicyDrivenResultSet(
//    private val originalResult: CompletionResultSet,
//    private val policyHolder: () -> ElementsAddingPolicy
//) : CompletionResultSet(originalResult.prefixMatcher, originalResult.consumer, originalResult.contributor) {
//
//    override fun addElement(element: LookupElement) {
//        policyHolder().addElement(originalResult, element)
//    }
//
//    override fun addAllElements(elements: MutableIterable<LookupElement>) {
//        policyHolder().addAllElements(originalResult, elements)
//    }
//
//    override fun withPrefixMatcher(matcher: PrefixMatcher): CompletionResultSet {
//        return PolicyDrivenResultSet(originalResult.withPrefixMatcher(matcher), policyHolder)
//    }
//
//    override fun withPrefixMatcher(prefix: String): CompletionResultSet {
//        return PolicyDrivenResultSet(originalResult.withPrefixMatcher(prefix), policyHolder)
//    }
//
//    override fun withRelevanceSorter(sorter: CompletionSorter): CompletionResultSet {
//        return PolicyDrivenResultSet(originalResult.withRelevanceSorter(sorter), policyHolder)
//    }
//
//    override fun addLookupAdvertisement(text: String) {
//        originalResult.addLookupAdvertisement(text)
//    }
//
//    override fun caseInsensitive(): CompletionResultSet {
//        return PolicyDrivenResultSet(originalResult.caseInsensitive(), policyHolder)
//    }
//
//    override fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>?) {
//        originalResult.restartCompletionOnPrefixChange(prefixCondition)
//    }
//
//    override fun restartCompletionWhenNothingMatches() {
//        originalResult.restartCompletionWhenNothingMatches()
//    }
//
//    override fun isStopped(): Boolean {
//        return originalResult.isStopped
//    }
//
//    override fun stopHere() {
//        policyHolder().onResultStop(originalResult)
//        originalResult.stopHere()
//    }
//}