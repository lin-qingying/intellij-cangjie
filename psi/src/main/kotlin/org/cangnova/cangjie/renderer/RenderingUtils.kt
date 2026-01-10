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

package org.cangnova.cangjie.renderer

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name

fun Name.render(): String {
    return if (this.shouldBeEscaped()) '`' + asString() + '`' else asString()
}

private fun Name.shouldBeEscaped(): Boolean {
    val string = asString()
    return string in KeywordStringsGenerated.KEYWORDS ||
        string.any { !Character.isLetterOrDigit(it) && it != '_' } ||
        string.isEmpty() ||
        !Character.isJavaIdentifierStart(string.codePointAt(0))
}

fun FqNameUnsafe.render(): String {
    return renderFqName(pathSegments())
}

fun FqName.render(): String {
    return renderFqName(pathSegments())
}

fun renderFqName(pathSegments: List<Name>): String {
    return buildString {
        for (element in pathSegments) {
            if (length > 0) {
                append(".")
            }
            append(element.render())
        }
    }
}

fun replacePrefixesInTypeRepresentations(
    lowerRendered: String,
    lowerPrefix: String,
    upperRendered: String,
    upperPrefix: String,
    foldedPrefix: String,
): String? {
    if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
        val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length)
        val upperWithoutPrefix = upperRendered.substring(upperPrefix.length)
        val flexibleCollectionName = foldedPrefix + lowerWithoutPrefix

        if (lowerWithoutPrefix == upperWithoutPrefix) return flexibleCollectionName

        if (typeStringsDifferOnlyInNullability(lowerWithoutPrefix, upperWithoutPrefix)) {
            return "$flexibleCollectionName!"
        }
    }
    return null
}

fun typeStringsDifferOnlyInNullability(lower: String, upper: String) =
    lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper
