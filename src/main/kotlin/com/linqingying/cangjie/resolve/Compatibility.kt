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

package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjTokens.*

enum class Compatibility {
    // modifier pair is compatible: ok (default)
    COMPATIBLE,

    // 第二个对第一个来说是多余的：警告
    REDUNDANT,

    //第一对第二来说是多余的：警告
    REVERSE_REDUNDANT,

    // error
    REPEATED,

    // pair is deprecated, will become incompatible: warning
    DEPRECATED,

    // pair is incompatible: error
    INCOMPATIBLE,

    // same but only for functions / properties: error
    COMPATIBLE_FOR_CLASSES_ONLY
}
// First modifier in pair should be also first in declaration
private val mutualCompatibility = buildCompatibilityMap()
private fun buildCompatibilityMap(): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<CjKeywordToken, CjKeywordToken>, Compatibility>()

    // Visibilities: incompatible
    result += incompatibilityRegister(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)

    // open is redundant to abstract & override
    result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, PUBLIC_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, OPEN_KEYWORD)


    // const is incompatible with abstract, open, override
    result += incompatibilityRegister(CONST_KEYWORD, ABSTRACT_KEYWORD)
    result += incompatibilityRegister(CONST_KEYWORD, OPEN_KEYWORD)
    result += incompatibilityRegister(CONST_KEYWORD, OVERRIDE_KEYWORD)

    // private is incompatible with override
    result += incompatibilityRegister(PRIVATE_KEYWORD, OVERRIDE_KEYWORD)
    // private is compatible with open / abstract only for classes
    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, OPEN_KEYWORD)
    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)



    return result
}
private fun compatibilityForClassesRegister(vararg list: CjKeywordToken) =
    compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

fun compatibility(first: CjKeywordToken, second: CjKeywordToken): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
    }
}
private fun compatibilityRegister(
    compatibility: Compatibility, vararg list: CjKeywordToken
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<CjKeywordToken, CjKeywordToken>, Compatibility>()
    for (first in list) {
        for (second in list) {
            if (first != second) {
                result[Pair(first, second)] = compatibility
            }
        }
    }
    return result
}
//注册不兼容
private fun incompatibilityRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}
//注册多余
private fun redundantRegister(
    sufficient:CjKeywordToken,
    redundant:CjKeywordToken
): Map<Pair<CjKeywordToken,CjKeywordToken>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}
