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

package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.ElementPattern
//
//class PolicyObeyingResultSet public  constructor(
//    private val originalResult: CompletionResultSet,
//    private val policyHolder: () -> ElementsAddingPolicy
//) : CompletionResultSet(
//    originalResult.prefixMatcher,
//    originalResult.consumer,
//    originalResult.contributor
//) {
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
//        return PolicyObeyingResultSet(originalResult.withPrefixMatcher(matcher), policyHolder)
//    }
//
//    override fun withPrefixMatcher(prefix: String): CompletionResultSet {
//        return PolicyObeyingResultSet(originalResult.withPrefixMatcher(prefix), policyHolder)
//    }
//
//    override fun withRelevanceSorter(sorter: CompletionSorter): CompletionResultSet {
//        return PolicyObeyingResultSet(originalResult.withRelevanceSorter(sorter), policyHolder)
//    }
//
//    override fun addLookupAdvertisement(text: String) {
//        originalResult.addLookupAdvertisement(text)
//    }
//
//    override fun caseInsensitive(): CompletionResultSet {
//        return PolicyObeyingResultSet(originalResult.caseInsensitive(), policyHolder)
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
