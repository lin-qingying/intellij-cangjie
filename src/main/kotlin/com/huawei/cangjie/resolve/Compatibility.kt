package com.huawei.cangjie.resolve

import com.huawei.cangjie.lexer.CjKeywordToken
import com.huawei.cangjie.lexer.CjTokens.*

enum class Compatibility {
    // modifier pair is compatible: ok (default)
    COMPATIBLE,

    // second is redundant to first: warning
    REDUNDANT,

    // first is redundant to second: warning
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
    // Abstract + open + final + sealed: incompatible
    result += incompatibilityRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD , SEALED_KEYWORD)


    // open is redundant to abstract & override
    result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
    // abstract is redundant to sealed
    result += redundantRegister(SEALED_KEYWORD, ABSTRACT_KEYWORD)

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

private fun incompatibilityRegister(vararg list: CjKeywordToken): Map<Pair<CjKeywordToken, CjKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

private fun redundantRegister(
    sufficient:CjKeywordToken,
    redundant:CjKeywordToken
): Map<Pair<CjKeywordToken,CjKeywordToken>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}
