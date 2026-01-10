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

package org.cangnova.cangjie.utils.exceptions

import org.cangnova.cangjie.utils.Printer

/**
 * 智能打印器，用于格式化输出文本
 *
 * 该类在 [Printer] 基础上提供了更智能的缩进控制功能。
 * 它会自动管理第一次打印时的缩进，后续连续打印则不会添加额外缩进，
 * 直到调用 [println] 换行后才会重置状态。
 *
 * 使用场景：
 * - 生成格式化的代码或文本
 * - 构建带缩进的多行输出
 * - 异常信息的格式化打印
 *
 * @param appendable 输出目标
 * @param indent 缩进字符串，默认为 4 个空格
 *
 * @see Printer
 */
class SmartPrinter(appendable: Appendable, indent: String = DEFAULT_INDENT) {
    companion object {
        /**
         * 默认缩进字符串（4 个空格）
         */
        private const val DEFAULT_INDENT = "    "
    }

    /**
     * 底层打印器实例
     */
    private val printer = Printer(appendable, indent)

    /**
     * 标记是否已经进行过打印操作（用于控制缩进）
     */
    private var notFirstPrint: Boolean = false

    /**
     * 打印内容（不换行）
     *
     * 第一次调用时会添加缩进，后续连续调用则不添加缩进，
     * 直到调用 [println] 换行后才会重置状态。
     *
     * @param objects 要打印的对象
     */
    fun print(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printWithNoIndent(*objects)
        } else {
            printer.print(*objects)
        }
        notFirstPrint = true
    }

    /**
     * 打印内容并换行
     *
     * 如果是连续打印，则不添加缩进；
     * 换行后会重置状态，下次 [print] 调用将添加缩进。
     *
     * @param objects 要打印的对象
     */
    fun println(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printlnWithNoIndent(*objects)
        } else {
            printer.println(*objects)
        }
        notFirstPrint = false
    }

    /**
     * 增加一级缩进
     */
    fun pushIndent() {
        printer.pushIndent()
    }

    /**
     * 减少一级缩进
     */
    fun popIndent() {
        printer.popIndent()
    }

    /**
     * 获取当前缩进级别（以缩进单位计）
     *
     * @return 当前缩进级别
     */
    fun getCurrentIndentInUnits() = printer.currentIndentLengthInUnits

    /**
     * 获取缩进单位的长度
     *
     * @return 缩进单位的字符数
     */
    fun getIndentUnit() = printer.indentUnitLength

    /**
     * 将打印内容转换为字符串
     *
     * @return 累积的打印内容
     */
    override fun toString(): String = printer.toString()
}

/**
 * 在指定的代码块中增加一级缩进
 *
 * 该函数会在执行代码块前增加缩进，执行完毕后自动恢复缩进级别。
 *
 * 示例：
 * ```kotlin
 * printer.println("class Foo {")
 * printer.withIndent {
 *     println("fun bar() {}")
 * }
 * printer.println("}")
 * ```
 *
 * @param block 要执行的代码块
 */
inline fun SmartPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
