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

import java.io.IOException

/**
 * 格式化打印器，用于生成带缩进的文本输出
 *
 * 该类提供了强大的文本格式化功能，支持：
 * - 自动缩进管理
 * - 空行数量控制
 * - 分隔符格式化
 * - 灵活的打印模式
 *
 * 使用场景：
 * - 代码生成
 * - 格式化文本输出
 * - 日志和调试信息的结构化输出
 * - 生成配置文件或报告
 *
 * @property out 输出目标
 * @property maxBlankLines 允许的最大连续空行数
 * @property indentUnit 每级缩进的字符串
 * @property indent 当前缩进字符串
 */
class Printer private constructor(
    private val out: Appendable,
    private val maxBlankLines: Int,
    private val indentUnit: String,
    private var indent: String
) {
    /**
     * 当前连续空行计数
     */
    private var blankLineCountIncludingCurrent = 0

    /**
     * 是否暂时不添加缩进（仅一次）
     */
    private var withholdIndentOnce = false

    /**
     * 已输出的字符总长度
     */
    private var length = 0

    /**
     * 创建打印器
     *
     * @param out 输出目标
     * @param indentUnit 缩进单位字符串
     */
    constructor(out: Appendable, indentUnit: String) : this(out, Int.Companion.MAX_VALUE, indentUnit)

    /**
     * 创建打印器
     *
     * @param out 输出目标
     * @param maxBlankLines 允许的最大连续空行数（默认不限制）
     * @param indentUnit 缩进单位字符串（默认为 4 个空格）
     */
    @JvmOverloads
    constructor(
        out: Appendable,
        maxBlankLines: Int = Int.Companion.MAX_VALUE,
        indentUnit: String = DEFAULT_INDENTATION_UNIT
    ) : this(out, maxBlankLines, indentUnit, "")

    /**
     * 从父打印器创建子打印器
     *
     * 子打印器会继承父打印器的配置。
     *
     * @param out 输出目标
     * @param parent 父打印器
     */
    constructor(out: Appendable, parent: Printer) : this(out, parent.maxBlankLines, parent.indentUnit, parent.indent)

    /**
     * 追加内容到输出
     *
     * @param o 要追加的对象
     */
    private fun append(o: Any) {
        try {
            val string = o.toString()
            out.append(string)
            length += string.length
        } catch (e: IOException) {
        }
    }

    /**
     * 打印内容并换行
     *
     * @param objects 要打印的对象
     * @return 当前打印器实例（支持链式调用）
     */
    fun println(vararg objects: Any): Printer {
        print(*objects)
        printLineSeparator()

        return this
    }

    /**
     * 打印行分隔符
     */
    private fun printLineSeparator() {
        if (blankLineCountIncludingCurrent <= maxBlankLines) {
            blankLineCountIncludingCurrent++
            append(LINE_SEPARATOR)
        }
    }

    /**
     * 打印内容（不换行）
     *
     * 如果没有调用 [withholdIndentOnce]，会自动添加缩进。
     *
     * @param objects 要打印的对象
     * @return 当前打印器实例（支持链式调用）
     */
    fun print(vararg objects: Any): Printer {
        if (withholdIndentOnce) {
            withholdIndentOnce = false
        } else if (objects.isNotEmpty()) {
            printIndent()
        }
        printWithNoIndent(*objects)

        return this
    }

    /**
     * 打印缩进
     */
    fun printIndent() {
        append(indent)
    }

    /**
     * 打印内容（不添加缩进）
     *
     * @param objects 要打印的对象
     * @return 当前打印器实例（支持链式调用）
     */
    fun printWithNoIndent(vararg objects: Any): Printer {
        for (`object` in objects) {
            blankLineCountIncludingCurrent = 0
            append(`object`)
        }

        return this
    }

    /**
     * 下次打印时不添加缩进（仅一次）
     *
     * @return 当前打印器实例（支持链式调用）
     */
    fun withholdIndentOnce(): Printer {
        withholdIndentOnce = true
        return this
    }

    /**
     * 打印内容并换行（不添加缩进）
     *
     * @param objects 要打印的对象
     * @return 当前打印器实例（支持链式调用）
     */
    fun printlnWithNoIndent(vararg objects: Any): Printer {
        printWithNoIndent(*objects)
        printLineSeparator()

        return this
    }

    /**
     * 增加一级缩进
     *
     * @return 当前打印器实例（支持链式调用）
     */
    fun pushIndent(): Printer {
        indent += indentUnit

        return this
    }

    /**
     * 减少一级缩进
     *
     * @return 当前打印器实例（支持链式调用）
     * @throws IllegalStateException 如果当前没有缩进可以弹出
     */
    fun popIndent(): Printer {
        check(indent.length >= indentUnit.length) { "No indentation to pop" }

        indent = indent.substring(indentUnit.length)

        return this
    }

    /**
     * 打印用分隔符分隔的多个项
     *
     * @param separator 分隔符
     * @param items 要打印的项
     * @return 当前打印器实例（支持链式调用）
     */
    fun separated(separator: Any, vararg items: Any?): Printer {
        for (i in items.indices) {
            if (i > 0) {
                printlnWithNoIndent(separator)
            }
            printlnWithNoIndent(items[i]!!)
        }
        return this
    }

    /**
     * 打印用分隔符分隔的集合项
     *
     * @param separator 分隔符
     * @param items 要打印的集合
     * @return 当前打印器实例（支持链式调用）
     */
    fun separated(separator: Any, items: MutableCollection<*>): Printer {
        val iterator: MutableIterator<*> = items.iterator()
        while (iterator.hasNext()) {
            printlnWithNoIndent(iterator.next()!!)
            if (iterator.hasNext()) {
                printlnWithNoIndent(separator)
            }
        }
        return this
    }

    /**
     * 检查是否没有输出任何内容
     */
    val isEmpty: Boolean
        get() = length == 0

    /**
     * 将打印内容转换为字符串
     *
     * @return 累积的打印内容
     */
    override fun toString(): String {
        return out.toString()
    }

    /**
     * 获取当前缩进级别（以缩进单位计）
     */
    val currentIndentLengthInUnits: Int
        get() = indent.length / indentUnit.length

    /**
     * 获取缩进单位的长度
     */
    val indentUnitLength: Int
        get() = indentUnit.length

    companion object {
        /**
         * 默认缩进单位（4 个空格）
         */
        private const val DEFAULT_INDENTATION_UNIT = "    "

        /**
         * 系统行分隔符
         */
        val LINE_SEPARATOR: String = System.lineSeparator()
    }
}