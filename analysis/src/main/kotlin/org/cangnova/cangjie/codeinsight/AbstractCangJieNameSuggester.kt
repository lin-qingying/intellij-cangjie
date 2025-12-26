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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.codeinsight.CangJieNameSuggester.MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.capitalizeAsciiOnly
import org.cangnova.cangjie.utils.decapitalizeAsciiOnly
abstract class AbstractCangJieNameSuggester {
    fun suggestNamesByFqName(
        fqName: FqName,
        ignoreCompanion: Boolean = true,
        validator: (String) -> Boolean = { true },
        defaultName: () -> String? = { null }
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        var name = ""
        fqName.asString().split('.').asReversed().forEach {
            if (ignoreCompanion && it == "Companion") return@forEach
            name = name.withPrefix(it)
            result.addName(name, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName(), validator)
        }

        return result
    }


    /**
     * Validates [name] and slightly improves it by adding a numeric suffix in case of conflicts.
     *
     * @param name to check in scope
     * @return [name] or nameI, where I is an integer
     */
    fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
        if (validator(name)) return name
        var i = 1
        while (i <= MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS && !validator(name + i)) {
            ++i
        }

        return name + i
    }
    fun suggestNamesForTypeParameters(count: Int, validator: (String) -> Boolean): List<String> {
        val result = ArrayList<String>()
        for (i in 0 until count) {
            result.add(suggestNameByMultipleNames(COMMON_TYPE_PARAMETER_NAMES, validator))
        }
        return result
    }

    fun suggestTypeAliasNameByPsi(typeElement: CjTypeElement, validator: (String) -> Boolean): String {
        fun CjTypeElement.render(): String {
            return when (this) {
                is CjOptionType -> "Option${getInnerType()?.render() ?: ""}"
                is CjFunctionType -> {
                    val arguments = listOfNotNull(receiverTypeReference) + parameters.mapNotNull { it.typeReference }
                    val argText = arguments.joinToString(separator = "") { it.typeElement?.render() ?: "" }
                    val returnText = returnTypeReference?.typeElement?.render() ?: "Unit"
                    "${argText}To$returnText"
                }
                is CjUserType -> {
                    val argText = typeArguments.joinToString(separator = "") { it.typeReference?.typeElement?.render() ?: "" }
                    "$argText${referenceExpression?.text ?: ""}"
                }
                else -> text.capitalizeAsciiOnly()
            }
        }

        return suggestNameByName(typeElement.render(), validator)
    }

    /**
     * Validates name using set of variants which are tried in succession (and extended with suffixes if necessary)
     * For example, when given sequence of a, b, c possible names are tried out in the following order: a, b, c, a1, b1, c1, a2, b2, c2, ...
     * @param names to check it in scope
     * @return name or nameI, where name is one of variants and I is a number
     */
    fun suggestNameByMultipleNames(names: Collection<String>, validator: (String) -> Boolean): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validator(candidate)) return candidate
            }
            i++
        }
    }

    protected fun MutableCollection<String>.addCamelNames(
        name: String,
        validator: (String) -> Boolean,
        mustStartWithLowerCase: Boolean = true
    ) {
        addAll(CangJieNameSuggester.getCamelNames(name, validator, mustStartWithLowerCase))
    }

    protected fun MutableCollection<String>.addNamesByExpressionPSI(expression: CjExpression?, validator: (String) -> Boolean) {
        addAll(CangJieNameSuggester.suggestNamesByExpressionPSI(expression, validator))
    }

    protected fun MutableCollection<String>.addName(name: String?, validator: (String) -> Boolean) {
        addIfNotNull(CangJieNameSuggester.suggestNameByValidIdentifierName(name, validator, false))
    }

    private fun String.withPrefix(prefix: String): String {
        if (isEmpty()) return prefix
        val c = this[0]
        return (if (c in 'a'..'z') prefix.decapitalizeAsciiOnly()
        else prefix.capitalizeAsciiOnly()) + capitalizeAsciiOnly()
    }

    companion object {
        private val COMMON_TYPE_PARAMETER_NAMES = listOf("T", "U", "V", "W", "X", "Y", "Z")
    }
}
