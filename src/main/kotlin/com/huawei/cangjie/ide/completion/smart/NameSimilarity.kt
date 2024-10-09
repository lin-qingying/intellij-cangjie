package com.huawei.cangjie.ide.completion.smart

import com.huawei.cangjie.ide.ExpectedInfo
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
