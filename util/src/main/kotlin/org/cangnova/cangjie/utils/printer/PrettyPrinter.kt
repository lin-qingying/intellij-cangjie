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

package org.cangnova.cangjie.utils.printer

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 格式化打印器，用于生成缩进良好的代码文本。
 *
 * 提供了各种辅助方法来简化代码生成过程，包括：
 * - 缩进管理
 * - 前缀/后缀处理
 * - 集合打印
 * - 分隔符处理
 *
 * @param indentSize 每级缩进的空格数
 */
class PrettyPrinter(val indentSize: Int = 4) : Appendable {

    @PublishedApi
    internal val builder: StringBuilder = StringBuilder()

    @PublishedApi
    internal var prefixesToPrint: PersistentList<String> = persistentListOf()

    @PublishedApi
    internal var indent: Int = 0

    override fun append(nullableSeq: CharSequence?): Appendable = apply {
        val seq = nullableSeq ?: "null"
        if (seq.isEmpty()) return@apply
        printPrefixes()
        seq.split('\n').forEachIndexed { index, line ->
            if (index > 0) {
                builder.append('\n')
            }

            // Skip indents if the line is empty.
            if (line.isNotEmpty()) {
                appendIndentIfNeeded()
                builder.append(line)
            }
        }
    }

    override fun append(nullableSeq: CharSequence?, start: Int, end: Int): Appendable = apply {
        append((nullableSeq ?: "null").subSequence(start, end))
    }

    override fun append(c: Char): Appendable = apply {
        printPrefixes()
        if (c != '\n') {
            appendIndentIfNeeded()
        }
        builder.append(c)
    }

    private fun printPrefixes() {
        if (prefixesToPrint.isNotEmpty()) {
            appendIndentIfNeeded()
            prefixesToPrint.forEach { builder.append(it) }
            prefixesToPrint = persistentListOf()
        }
    }

    /**
     * 追加一行（带换行）。
     */
    fun appendLine(text: String = ""): PrettyPrinter {
        append(text)
        builder.append('\n')
        return this
    }

    inline fun withIndent(block: PrettyPrinter.() -> Unit) {
        indent += 1
        block(this)
        indent -= 1
    }

    inline fun withIndents(indentCount: Int, block: PrettyPrinter.() -> Unit) {
        require(indentCount >= 0) { "Number of indents should be non-negative" }
        indent += indentCount
        block(this)
        indent -= indentCount
    }

    inline fun withIndentInBraces(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "{", after = "}", block)
    }

    inline fun withIndentInSquareBrackets(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "[", after = "]", block)
    }

    inline fun withIndentWrapped(before: String, after: String, block: PrettyPrinter.() -> Unit) {
        append(before)
        appendLine()
        withIndent(block)
        appendLine()
        append(after)
    }

    inline fun <T> printCollection(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
        append(prefix)
        val iterator = collection.iterator()
        while (iterator.hasNext()) {
            renderItem(iterator.next())
            if (iterator.hasNext()) {
                append(separator)
            }
        }
        append(postfix)
    }

    inline fun <T> printCollectionIfNotEmpty(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
        if (!collection.iterator().hasNext()) return
        printCollection(collection, separator, prefix, postfix, renderItem)
    }

    fun printCharIfNotThere(char: Char) {
        if (builder.lastOrNull() != char) {
            append(char)
        }
    }

    private fun appendIndentIfNeeded() {
        if (builder.isEmpty() || builder[builder.lastIndex] == '\n') {
            builder.append(" ".repeat(indentSize * indent))
        }
    }

    override fun toString(): String {
        return builder.toString()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun checkIfPrinted(render: () -> Unit): Boolean {
        contract { callsInPlace(render, InvocationKind.EXACTLY_ONCE) }
        val initialSize = builder.length
        render()
        return initialSize != builder.length
    }

    inline operator fun invoke(print: PrettyPrinter.() -> Unit) {
        this.print()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
        }
        val firstRendered = checkIfPrinted { p1() }
        if (firstRendered) {
            withPrefix(this, p2)
        } else {
            p2()
        }
    }

    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2) }, p3)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p4, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3) }, p4)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit, p5: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p4, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p5, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3, p4) }, p5)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun withPrefix(prefix: String, print: () -> Unit) {
        contract {
            callsInPlace(print, InvocationKind.EXACTLY_ONCE)
        }
        val currentPrefixes = prefixesToPrint
        prefixesToPrint = prefixesToPrint.add(prefix)
        try {
            print()
        } finally {
            if (prefixesToPrint.isNotEmpty()) {
                prefixesToPrint = currentPrefixes
            }
        }
    }

    inline fun withSuffix(suffix: String, p1: () -> Unit) {
        checkIfPrinted { p1() }.ifTrue { append(suffix) }
    }
}

/**
 * 创建一个 PrettyPrinter 并执行打印操作。
 */
@OptIn(ExperimentalContracts::class)
inline fun prettyPrint(body: PrettyPrinter.() -> Unit): String {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return PrettyPrinter().apply(body).toString()
}

/**
 * 使用另一个 PrettyPrinter 的设置创建新的打印器。
 */
@OptIn(ExperimentalContracts::class)
inline fun prettyPrintWithSettingsFrom(other: PrettyPrinter, body: PrettyPrinter.() -> Unit): String {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return PrettyPrinter(other.indentSize).apply(body).toString()
}

/**
 * 如果为 true，则执行代码块。
 */
inline fun Boolean.ifTrue(block: () -> Unit) {
    if (this) block()
}