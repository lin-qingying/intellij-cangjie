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

package org.cangnova.cangjie.utils

import java.lang.StringBuilder

fun String.toUpperCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'a'..'z') c.uppercaseChar() else c)
    }
    return builder.toString()
}

fun String.capitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'a'..'z') {
        buildString(length) {
            append(c.uppercaseChar())
            append(this@capitalizeAsciiOnly, 1, this@capitalizeAsciiOnly.length)
        }
    } else {
        this
    }
}

fun String.toLowerCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'A'..'Z') c.lowercaseChar() else c)
    }
    return builder.toString()
}

/**
 * 将字符串以智能方式首字母小写（处理驼峰与下划线等情况）。
 * 示例：
 * - "FooBar" -> "fooBar"
 * - "FOOBar" -> "fooBar"
 * - "FOO" -> "foo"
 * - "FOO_BAR" -> "fooBar"
 * - "__F_BAR" -> "fBar"
 */
fun String.decapitalizeSmart(asciiOnly: Boolean = false): String {
    return decapitalizeWithUnderscores(this, asciiOnly)
        ?: decapitalizeSmartForCompiler(asciiOnly)
}

/**
 * 根据下划线分隔的单词列表进行智能小写化转换；如果字符串不包含下划线或只有一个词则返回 null。
 * 示例：
 * - "FOO_BAR" -> "fooBar"
 * - "FOO_BAR_BAZ" -> "fooBarBaz"
 * - "__F_BAR" -> "fBar"
 */
private fun decapitalizeWithUnderscores(str: String, asciiOnly: Boolean): String? {
    val words = str.split("_").filter { it.isNotEmpty() }

    if (words.size <= 1) return null

    val builder = StringBuilder()

    words.forEachIndexed { index, word ->
        if (index == 0) {
            builder.append(toLowerCase(word, asciiOnly))
        } else {
            builder.append(toUpperCase(word.first().toString(), asciiOnly))
            builder.append(toLowerCase(word.drop(1), asciiOnly))
        }
    }

    return builder.toString()
}

private fun toLowerCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toLowerCaseAsciiOnly() else string.lowercase()
}

private fun toUpperCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toUpperCaseAsciiOnly() else string.uppercase()
}

/**
 * 为编译器提供的智能小写化处理：当字符串以大写字母序列开头时，保留语义正确的分界并小写前缀。
 * 示例：
 * - "FooBar" -> "fooBar"
 * - "FOOBar" -> "fooBar"
 * - "FOO" -> "foo"
 */
fun String.decapitalizeSmartForCompiler(asciiOnly: Boolean = false): String {
    if (isEmpty() || !isUpperCaseCharAt(0, asciiOnly)) return this

    if (length == 1 || !isUpperCaseCharAt(1, asciiOnly)) {
        return if (asciiOnly) decapitalizeAsciiOnly() else replaceFirstChar(Char::lowercaseChar)
    }

    val secondWordStart = (indices.firstOrNull { !isUpperCaseCharAt(it, asciiOnly) } ?: return toLowerCase(this, asciiOnly)) - 1

    return toLowerCase(substring(0, secondWordStart), asciiOnly) + substring(secondWordStart)
}

private fun String.isUpperCaseCharAt(index: Int, asciiOnly: Boolean): Boolean {
    val c = this[index]
    return if (asciiOnly) c in 'A'..'Z' else c.isUpperCase()
}
fun String.decapitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'A'..'Z') {
        c.lowercaseChar() + substring(1)
    } else {
        this
    }
}
