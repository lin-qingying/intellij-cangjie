/*
 * Copyright 2025 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.utils

import com.intellij.openapi.util.text.StringUtil
import kotlin.math.max
import kotlin.math.min
import kotlin.text.iterator

private const val CARET_MARKER = "<~!!~>"
private const val BEGIN_MARKER = "<~BEGIN~>"
private const val END_MARKER = "<~END~>"
fun String.collapseSpaces(): String {
    val builder = StringBuilder()
    var haveSpaces = false
    for (c in this) {
        if (c.isWhitespace()) {
            haveSpaces = true
        } else {
            if (haveSpaces) {
                builder.append(" ")
                haveSpaces = false
            }
            builder.append(c)
        }
    }
    return builder.toString()
}
fun CharSequence.substringWithContext(beginIndex: Int, endIndex: Int, range: Int): String {
    val start = max(0, beginIndex - range)
    val end = min(this.length, endIndex + range)

    val notFromBegin = start != 0
    val notToEnd = end != this.length

    val updatedStart = beginIndex - start
    val updatedEnd = endIndex - start

    return StringBuilder(this.toString().substring(start, end))
        .insert(updatedEnd, if (updatedEnd == updatedStart) CARET_MARKER else END_MARKER)
        .insert(updatedStart, if (updatedEnd == updatedStart) "" else BEGIN_MARKER)
        .insert(0, if (notFromBegin) "<~...${position(this, start)}~>" else "")
        .append(if (notToEnd) "<~${position(this, end)}...~>" else "").toString()
}

private fun position(str: CharSequence, offset: Int): String {
    val line = StringUtil.offsetToLineNumber(str, offset) + 1
    return "(line: $line)"
}

fun join(collection: Iterable<Any>, separator: String) = collection.joinToString(separator)
