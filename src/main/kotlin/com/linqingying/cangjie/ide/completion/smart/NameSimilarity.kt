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

package com.linqingying.cangjie.ide.completion.smart

import com.linqingying.cangjie.ide.ExpectedInfo
import com.intellij.openapi.util.Key
import com.intellij.psi.codeStyle.NameUtil
import kotlin.math.min

val NAME_SIMILARITY_KEY: Key<Int> = Key<Int>("NAME_SIMILARITY_KEY")
fun calcNameSimilarity(name: String, expectedInfos: Collection<ExpectedInfo>): Int =
    expectedInfos.mapNotNull { it.expectedName }.maxOfOrNull { calcNameSimilarity(name, it) } ?: 0

private fun calcNameSimilarity(name: String, expectedName: String): Int {
    val words1 = NameUtil.nameToWordsLowerCase(name)
    val words2 = NameUtil.nameToWordsLowerCase(expectedName)

    val matchedWords = words1.toSet().intersect(words2)
    if (matchedWords.isEmpty()) return 0

    fun isNonNumber(word: String) = !word[0].isDigit()
    val nonNumberWords1 = words1.filter(::isNonNumber)
    val nonNumberWords2 = words2.filter(::isNonNumber)

    // count number of words matched at the end (but ignore number words - they are less important)
    val minWords = min(nonNumberWords1.size, nonNumberWords2.size)
    val matchedTailLength = (0 until minWords).firstOrNull { i ->
        nonNumberWords1[nonNumberWords1.size - i - 1] != nonNumberWords2[nonNumberWords2.size - i - 1]
    } ?: minWords

    return matchedWords.size * 1000 + matchedTailLength
}
