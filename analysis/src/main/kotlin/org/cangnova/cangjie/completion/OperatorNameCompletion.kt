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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.COMPARE_GTEQ
import org.cangnova.cangjie.name.OperatorNameConventions.COMPARE_LT
import org.cangnova.cangjie.name.OperatorNameConventions.COMPARE_LTEQ
import org.cangnova.cangjie.name.OperatorNameConventions.CONTAINS
import org.cangnova.cangjie.name.OperatorNameConventions.EQUALS
import org.cangnova.cangjie.name.OperatorNameConventions.GET
import org.cangnova.cangjie.name.OperatorNameConventions.COMPARE_GT

import org.cangnova.cangjie.name.OperatorNameConventions.INVOKE
import org.cangnova.cangjie.name.OperatorNameConventions.NOT_EQUALS
import org.cangnova.cangjie.name.OperatorNameConventions.SET

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.cangnova.cangjie.name.OperatorConventions


object OperatorNameCompletion {

    private val additionalOperatorPresentation = mapOf(
        SET to "[...] = ...",
        GET to "[...]",
        CONTAINS to "in ",
        COMPARE_GT to ">",
        COMPARE_LT to "<",
                COMPARE_GTEQ to ">=",
                COMPARE_LTEQ to "<=",
        EQUALS to "==",
        NOT_EQUALS to "!=",
        INVOKE to "(...)"
    )

    private fun buildLookupElement(opName: Name): LookupElement {
        val element = LookupElementBuilder.create(opName)

        val symbol =
            (OperatorConventions.getOperationSymbolForName(opName) as? CjSingleValueToken)?.value ?: additionalOperatorPresentation[opName]

        if (symbol != null) return element.withTypeText(symbol)
        return element
    }

    fun doComplete(collector: LookupElementsCollector, descriptorNameFilter: (String) -> Boolean) {
        collector.addElements(OperatorConventions.CONVENTION_NAMES.filter { descriptorNameFilter(it.asString()) }
            .map(this::buildLookupElement))
    }
}
