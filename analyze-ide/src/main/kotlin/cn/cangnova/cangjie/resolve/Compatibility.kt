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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.lexer.CjKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens.*

enum class Compatibility {
    // modifier pair is compatible: ok (default)
    COMPATIBLE,

    // 第二个对第一个来说是多余的：警告
    REDUNDANT,

    // 第一个对第二个来说是多余的：警告
    REVERSE_REDUNDANT,

    // 错误
    REPEATED,

    // pair is deprecated, will become incompatible: 警告
    DEPRECATED,

    // pair is incompatible: 错误
    INCOMPATIBLE,

    // 同上，但仅适用于函数/属性：错误
    COMPATIBLE_FOR_CLASSES_ONLY
}

// 第一个修饰符在对中应先于声明中的第二个修饰符
private val mutualCompatibility = buildCompatibilityMap()

/**
 * 构建修饰符对之间的兼容性映射。
 *
 * 该函数初始化一个哈希映射，用于存储修饰符对之间的兼容性关系。
 *
 * @return 包含修饰符对及其兼容性的映射
 */
private fun buildCompatibilityMap(): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<CjKeywordToken, CjKeywordToken>, Compatibility>()

    // 可见性：不兼容
    result += incompatibilityRegister(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)

    // open 对于 abstract 和 override 是多余的
    result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, PUBLIC_KEYWORD)
    result += redundantRegister(SEALED_KEYWORD, OPEN_KEYWORD)

    // const 不兼容于 abstract, open, override
    result += incompatibilityRegister(CONST_KEYWORD, ABSTRACT_KEYWORD)
    result += incompatibilityRegister(CONST_KEYWORD, OPEN_KEYWORD)
//    result += incompatibilityRegister(CONST_KEYWORD, OVERRIDE_KEYWORD)

    // private 不兼容于 override
    result += incompatibilityRegister(PRIVATE_KEYWORD, OVERRIDE_KEYWORD)
//    redef 不兼容于 override
    result += incompatibilityRegister(REDEF_KEYWORD, OVERRIDE_KEYWORD)
//    static 不兼容 override
    result += incompatibilityRegister(STATIC_KEYWORD, OVERRIDE_KEYWORD)
//    open 不兼容 redef
    result += incompatibilityRegister(OPEN_KEYWORD, REDEF_KEYWORD)
//    open 不兼容 static
    result += incompatibilityRegister(STATIC_KEYWORD, OPEN_KEYWORD)


    // private 仅对于类与 open/abstract 兼容
    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, OPEN_KEYWORD)
    result += compatibilityForClassesRegister(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)

    return result
}


/**
 * 注册仅适用于类的兼容性修饰符对。
 *
 * @param list 修饰符令牌列表
 */
private fun compatibilityForClassesRegister(vararg list: CjKeywordToken) =
    compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

/**
 * 判断两个关键字令牌之间的兼容性。
 *
 * @param first 第一个关键字令牌
 * @param second 第二个关键字令牌
 * @return 返回两个关键字令牌之间的兼容性状态
 */
fun compatibility(first: CjKeywordToken, second: CjKeywordToken): Compatibility {
    // 如果两个关键字令牌相同，则返回重复状态
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        // 否则，从互斥兼容性映射中查找兼容性状态，如果没有找到则默认为兼容状态
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
    sufficient: CjKeywordToken,
    redundant: CjKeywordToken
): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}
