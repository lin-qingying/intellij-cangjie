/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion.turboComplete

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
