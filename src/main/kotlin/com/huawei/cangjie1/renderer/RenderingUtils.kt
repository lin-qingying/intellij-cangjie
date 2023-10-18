package com.huawei.cangjie1.renderer

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.name.FqNameUnsafe
import com.huawei.cangjie1.name.Name


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
    foldedPrefix: String
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

