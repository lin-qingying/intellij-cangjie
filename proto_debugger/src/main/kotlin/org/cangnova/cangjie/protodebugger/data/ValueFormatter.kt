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

package org.cangnova.cangjie.protodebugger.data

import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
import java.math.BigInteger

/**
 * 值格式化器
 *
 * 负责根据调试器设置格式化值的显示，包括十六进制格式化等。
 */
object ValueFormatter {

    /**
     * 检查字符串是否为十进制整数
     */
    private fun isDecimalInteger(s: String): Boolean {
        if (s.isEmpty()) return false

        var startIndex = 0
        // 允许负号
        if (s[0] == '-' || s[0] == '+') {
            if (s.length == 1) return false
            startIndex = 1
        }

        // 检查是否全是数字
        for (i in startIndex until s.length) {
            if (!s[i].isDigit()) return false
        }

        return true
    }

    /**
     * 将十进制字符串转换为十六进制
     */
    private fun decimalToHex(decimal: String): String {
        return try {
            val number = BigInteger(decimal)
            if (number.signum() < 0) {
                // 负数显示为有符号十六进制
                "-0x${number.abs().toString(16).uppercase()}"
            } else {
                "0x${number.toString(16).uppercase()}"
            }
        } catch (e: NumberFormatException) {
            decimal
        }
    }

    /**
     * 格式化值以供显示
     *
     * 根据调试器设置，可能会添加十六进制表示。
     *
     * @param value 原始值字符串
     * @param summary 摘要字符串（可选）
     * @return 格式化后的字符串
     */
    fun formatValue(value: String, summary: String? = null): String {
        val settings = DebuggerSettings.getInstance()
        val displayValue = summary ?: value

        // 如果启用了十六进制格式化
        if (settings.isHexFormattingEnabled()) {
            // 如果值已经是十六进制（以0x开头），直接返回
            if (displayValue.startsWith("0x", ignoreCase = true)) {
                return displayValue
            }

            // 如果值是十进制整数，转换为十六进制
            if (isDecimalInteger(displayValue)) {
                return decimalToHex(displayValue)
            }
        }

        // 如果启用了次要十六进制格式化
        if (settings.isHexAsSecondaryFormattingEnabled() && !settings.isHexFormattingEnabled()) {
            // 不是主要格式，作为次要值显示
            if (isDecimalInteger(displayValue)) {
                val hexValue = decimalToHex(displayValue)
                return "$displayValue ($hexValue)"
            }
        }

        return displayValue
    }

    /**
     * 格式化寄存器值
     *
     * 寄存器值通常以十六进制显示更有意义
     */
    fun formatRegisterValue(value: String): String {
        val settings = DebuggerSettings.getInstance()

        // 寄存器值默认就显示十六进制
        if (value.startsWith("0x", ignoreCase = true)) {
            return value
        }

        // 如果启用了十六进制格式化或次要格式化，为十进制寄存器值添加十六进制表示
        if ((settings.isHexFormattingEnabled() || settings.isHexAsSecondaryFormattingEnabled())
            && isDecimalInteger(value)
        ) {
            val hexValue = decimalToHex(value)
            return if (settings.isHexFormattingEnabled()) {
                hexValue
            } else {
                "$value ($hexValue)"
            }
        }

        return value
    }
}