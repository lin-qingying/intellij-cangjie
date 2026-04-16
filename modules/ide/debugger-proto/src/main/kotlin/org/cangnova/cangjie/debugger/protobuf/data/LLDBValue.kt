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

package org.cangnova.cangjie.debugger.protobuf.data

import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.debugger.protobuf.core.DebuggerDriverFacade.Companion.parseAddressSafe
import org.cangnova.cangjie.debugger.protobuf.exception.DebuggerCommandExceptionException
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import java.math.BigInteger
import java.util.regex.Pattern

/**
 * 调试器值数据类
 *
 * 该类表示调试器中值的详细数据信息，包含值的实际内容、描述、
 * 结构信息等，专门用于UI展示和值操作。
 *
 * 使用场景：
 * - 存储表达式求值的结果数据
 * - 分析值的类型和结构信息
 * - 处理指针相关的操作
 * - 判断值的布尔值和数值
 * - 在调试器UI中显示值的详细信息
 *
 * @param variableId 关联的变量ID
 * @param value 值的字符串表示（对应 SBValue::GetValue()）
 * @param summary 值的摘要（对应 SBValue::GetSummary()，适用于复杂对象）
 * @param valueDidChange 值是否发生了变化（对应 SBValue::GetValueDidChange()）
 * @param error 错误信息（对应 SBValue::GetError()）
 */
class LLDBValue(
    val variableId: Long,
    val value: String,
    val summary: String? = null,
    val valueDidChange: Boolean = false,
    val error: String? = null
) {
    companion object {
        /**
         * 十六进制值的正则表达式模式
         * 用于匹配字符串开头的十六进制数值，如"0x1234ABCD"
         */
        private val HEX_VALUE_PATTERN = Pattern.compile("^(0x\\p{XDigit}+)\\b")

        /**
         * 空指针的正则表达式模式
         * 用于匹配全为零的十六进制地址，如"0x0", "0x0000"
         */
        private val NULL_POINTER_PATTERN = Pattern.compile("0x0+")

        /**
         * 检查指针是否为空指针
         *
         * @param pointer 要检查的指针字符串
         * @return 如果是空指针返回true，否则返回false
         */
        private fun isNullPointer(pointer: String?): Boolean {
            return pointer != null && NULL_POINTER_PATTERN.matcher(pointer).matches()
        }

        /**
         * 从值字符串中提取指针
         *
         * 从包含指针的字符串中提取第一个十六进制数值作为指针。
         * 如果字符串中没有有效的十六进制指针，则返回null。
         *
         * @param value 包含指针的字符串
         * @return 提取的指针字符串，如果没有找到则返回null
         */
        private fun getPointer(value: String?): String? {
            if (value == null) {
                return null
            }

            val matches = StringUtil.findMatches(value, HEX_VALUE_PATTERN)
            return if (matches.size == 1) matches[0] else null
        }

        /**
         * 获取可显示的指针格式
         *
         * 将指针字符串格式化为更简洁的可显示格式，移除前导零。
         * 空指针特殊处理为"0x0"。
         *
         * 使用场景：
         * - 在调试器UI中显示指针地址
         * - 生成用户友好的地址表示
         * - 简化长地址的显示
         *
         * @param pointer 原始指针字符串
         * @return 格式化后的可显示指针
         */
        fun getPresentablePointer(pointer: String): String {

            var pointertemp = pointer

            if (isNullPointer(pointer)) {
                return "0x0"
            }

            var i = 2
            while (i < pointertemp.length && pointertemp[i] == '0') {
                i++
            }

            if (i > 2) {
                pointertemp = "0x${pointertemp.substring(i)}"
            }

            return pointertemp
        }

        /**
         * 检查字符串是否为有效的数字
         *
         * @param s 要检查的字符串
         * @return 如果可以解析为数字返回true，否则返回false
         */
        private fun isNum(s: String): Boolean {
            return try {
                BigInteger(s)
                true
            } catch (e: NumberFormatException) {
                false
            }
        }
    }

    /**
     * 检查是否有错误信息
     *
     * @return 如果有错误信息返回true，否则返回false
     */
    fun hasError(): Boolean = !error.isNullOrEmpty()

    /**
     * 获取可显示的值
     *
     * 返回用于UI显示的值字符串。优先使用summary，如果没有则使用value。
     * 根据调试器设置自动应用十六进制格式化。
     *
     * @return 可显示的值字符串
     */
    fun getPresentableValue(): String {
        return ValueFormatter.formatValue(value, summary)
    }


    /**
     * 检查值是否为指针类型
     *
     * 通过分析值的字符串表示来判断是否包含有效的内存地址。
     * 指针值通常以十六进制格式表示。
     *
     * @return 如果是指针类型返回true，否则返回false
     */
    fun isPointer(): Boolean {
        return getPointerOrNull() != null
    }

    /**
     * 检查是否为有效的非空指针
     *
     * 判断值是否为指针且不为空指针（地址不为0）。
     * 用于确定指针是否指向有效的内存地址。
     *
     * @return 如果是有效的非空指针返回true，否则返回false
     */
    fun isValidPointer(): Boolean {
        val pointer = getPointerOrNull()
        return pointer != null && !NULL_POINTER_PATTERN.matcher(pointer).matches()
    }

    /**
     * 检查是否为空指针
     *
     * 判断值是否为空指针（地址为0）。空指针是特殊的指针值，
     * 表示不指向任何有效的内存位置。
     *
     * @return 如果是空指针返回true，否则返回false
     */
    fun isNullPointer(): Boolean {
        return isNullPointer(getPointerOrNull())
    }

    /**
     * 获取指针字符串
     *
     * 获取值的指针表示。如果值不是有效的指针，将抛出异常。
     * 该方法用于确保调用方能够处理非指针值的情况。
     *
     * @return 指针字符串
     * @throws DebuggerCommandExceptionException 当值不是有效指针时
     */
    fun getPointer(): String {
        val result = getPointerOrNull()
        return result ?: throw DebuggerCommandExceptionException("Value is not a valid pointer: $value")
    }

    /**
     * 获取指针字符串（可为空）
     *
     * 尝试从值的字符串表示中提取指针信息。
     * 如果值不是指针类型，则返回null而不是抛出异常。
     *
     * @return 指针字符串，如果不是指针则返回null
     */
    fun getPointerOrNull(): String? {
        return getPointer(value)
    }

    /**
     * 获取可显示的指针格式
     *
     * 如果值是指针，返回格式化后的可显示指针格式；
     * 如果不是指针，则返回null。
     *
     * @return 格式化后的指针字符串，如果不是指针则返回null
     */
    fun getPresentablePointer(): String? {
        val pointer = getPointerOrNull()
        return pointer?.let { getPresentablePointer(it) }
    }

    /**
     * 获取指针地址对象
     *
     * 将指针字符串转换为Address对象。如果指针为空，
     * 则返回Address.NULL；如果不是指针则返回null。
     *
     * @return Address对象，如果不是指针则返回null
     */
    fun getPointerAddress(): Address? {
        val pointer = getPointerOrNull()
        return if (pointer == null) null else if (isNullPointer(pointer)) Address.NULL else try {
            Address.Companion.Parser.hex(pointer)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * 分离数值和描述信息
     *
     * 将值的描述字符串分离为数值部分和描述部分。
     * 常用于处理"数值 描述"格式的字符串。
     *
     * 使用场景：
     * - 解析枚举值的显示
     * - 分离数值和文本描述
     * - 提取数值部分进行计算
     *
     * @return Pair对象，第一个元素为数值部分（可能为null），第二个元素为描述部分
     */
    fun splitNumberAndData(): Pair<String?, String> {
        val displayText = summary ?: value
        val spaceIndex = displayText.indexOf(' ')
        return if (spaceIndex >= 0) {
            val numPart = displayText.take(spaceIndex)
            if (isNum(numPart)) Pair(numPart, displayText.substring(spaceIndex + 1).trim()) else Pair(null, displayText)
        } else {
            if (isNum(displayText)) Pair(displayText, "") else Pair(null, displayText)
        }
    }

    /**
     * 判断值是否为真
     *
     * 根据值的字符串表示判断其在布尔上下文中是否为真。
     * 该方法是isFalse()的反向操作。
     *
     * @return 如果值为真返回true，否则返回false
     */
    fun isTrue(): Boolean {
        return !isFalse()
    }

    /**
     * 判断值是否为假
     *
     * 根据值的字符串表示判断其在布尔上下文中是否为假。
     * 支持多种表示形式，包括数值、布尔值、字符等。
     *
     * @return 如果值为假返回true，否则返回false
     */
    fun isFalse(): Boolean {
        val s = arrayOf("0x0+", "0+", "false", "NO", "'\\\\0'", "0 [LUu]?'.*'")
        for (s1 in s) {
            if (Pattern.compile(s1).matcher(value).matches()) {
                return true
            }
        }
        return false
    }

    /**
     * 获取整数值
     *
     * 将值的字符串表示转换为长整型数值。
     * 如果值是指针，则解析为地址；否则直接解析数值。
     *
     * @return 长整型数值
     * @throws DebuggerCommandExceptionException 当值不能转换为有效整数时
     */
    fun intValue(): Long {
        return try {
            val pointer = getPointerOrNull()
            if (pointer != null) parseAddressSafe(pointer).value.toLong() else BigInteger(
                value
            )
                .toLong()
        } catch (numberException: NumberFormatException) {
            throw DebuggerCommandExceptionException("Value is not a valid integer: $value")
        }
    }

    override fun toString(): String {
        val errorInfo = error?.let { " [error: $it]" } ?: ""
        val changeInfo = if (valueDidChange) " [changed]" else ""
        val summaryInfo = summary?.let { " (summary: $it)" } ?: ""
        return "Value($value)$summaryInfo$changeInfo$errorInfo"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val valueData = other as LLDBValue
        if (variableId != valueData.variableId) return false
        if (value != valueData.value) return false
        if (summary != valueData.summary) return false
        if (valueDidChange != valueData.valueDidChange) return false
        if (error != valueData.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variableId.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (summary?.hashCode() ?: 0)
        result = 31 * result + valueDidChange.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

/**
 * Creates an LLDBValue from a protobuf Model.Value
 *
 * @param value The protobuf value from LLDB
 * @return Configured LLDBValue instance
 */
fun createLLDBValue(value: lldbprotobuf.Model.Value): LLDBValue {
    return LLDBValue(
        variableId = value.variableId.id,
        value = value.value,
        summary = value.summary.takeIf { it.isNotEmpty() },
        valueDidChange = value.valueDidChange,
        error = value.error.takeIf { it.isNotEmpty() }
    )
}
