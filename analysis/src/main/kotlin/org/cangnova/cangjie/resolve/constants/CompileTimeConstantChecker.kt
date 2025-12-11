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

package org.cangnova.cangjie.resolve.constants

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.diagnostics.infos.errors.CONSTANT_EXPECTED_TYPE_MISMATCH
import org.cangnova.cangjie.diagnostics.infos.errors.EMPTY_CHARACTER_LITERAL
import org.cangnova.cangjie.diagnostics.infos.errors.FLOAT_LITERAL_OUT_OF_RANGE
import org.cangnova.cangjie.diagnostics.infos.errors.ILLEGAL_ESCAPE
import org.cangnova.cangjie.diagnostics.infos.errors.INCORRECT_CHARACTER_LITERAL
import org.cangnova.cangjie.diagnostics.infos.errors.INT_LITERAL_OUT_OF_RANGE
import org.cangnova.cangjie.diagnostics.infos.errors.INT_LITERAL_OUT_OF_RANGE_BY_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.isError

/**
 * 编译时常量检查器
 */
class CompileTimeConstantChecker(
    private val context: ResolutionContext<*>,
    private val module: ModuleDescriptor,
    private val checkOnlyErrorsThatDependOnExpectedType: Boolean
) {
    private val builtIns: CangJieBuiltIns = module.builtIns
    private val trace: BindingTrace = context.trace

    /**
     * 检查常量表达式类型
     * @return 如果有错误返回 true
     */
    fun checkConstantExpressionType(
        compileTimeConstant: ConstantValue<*>?,
        expression: CjConstantExpression,
        expectedType: CangJieType
    ): Boolean {
        return when (expression.node.elementType) {
            CjNodeTypes.INTEGER_CONSTANT -> checkIntegerValue(compileTimeConstant, expectedType, expression)
            CjNodeTypes.FLOAT_CONSTANT -> checkFloatValue(compileTimeConstant, expectedType, expression)
            CjNodeTypes.BOOLEAN_CONSTANT -> checkBooleanValue(expectedType, expression)
            CjNodeTypes.RUNE_CONSTANT -> checkCharValue(compileTimeConstant, expectedType, expression)
            else -> false
        }
    }

    private fun checkIntegerValue(
        value: ConstantValue<*>?,
        expectedType: CangJieType,
        expression: CjConstantExpression
    ): Boolean {
        if (value == null) {
            return reportError(INT_LITERAL_OUT_OF_RANGE.on(expression))
        }

        // UInt64通过value是否为空来判断是否越界
        if (!CangJieBuiltIns.isUInt64(expectedType) && CangJieBuiltIns.isNumber(expectedType)) {
            val maxValue = expectedType.maxValue()
            try {
                val longValue = value.value.toString().toLong()
                if (longValue > maxValue) {
                    reportError(INT_LITERAL_OUT_OF_RANGE_BY_TYPE.on(expression, longValue, expectedType))
                }
            } catch (ignored: Exception) {
                // 忽略解析错误
            }
        }

        if (!noExpectedTypeOrError(expectedType)) {
            val valueType = value.getType(module)
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
                return reportConstantExpectedTypeMismatch(expression, "integer", expectedType, null)
            }
        }
        return false
    }

    private fun checkFloatValue(
        value: ConstantValue<*>?,
        expectedType: CangJieType,
        expression: CjConstantExpression
    ): Boolean {
        if (value == null) {
            return reportError(FLOAT_LITERAL_OUT_OF_RANGE.on(expression))
        }
        if (!noExpectedTypeOrError(expectedType)) {
            val valueType = value.getType(module)
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
                return reportConstantExpectedTypeMismatch(expression, "floating-point", expectedType, null)
            }
        }
        return false
    }

    private fun checkBooleanValue(
        expectedType: CangJieType,
        expression: CjConstantExpression
    ): Boolean {
        if (!noExpectedTypeOrError(expectedType) &&
            !CangJieTypeChecker.DEFAULT.isSubtypeOf(builtIns.boolType, expectedType)
        ) {
            return reportConstantExpectedTypeMismatch(expression, "Bool", expectedType, builtIns.boolType)
        }
        return false
    }

    private fun checkCharValue(
        constant: ConstantValue<*>?,
        expectedType: CangJieType,
        expression: CjConstantExpression
    ): Boolean {
        if (!noExpectedTypeOrError(expectedType) &&
            !CangJieTypeChecker.DEFAULT.isSubtypeOf(builtIns.runeType, expectedType)
        ) {
            return reportConstantExpectedTypeMismatch(expression, "character", expectedType, builtIns.runeType)
        }

        if (constant != null) {
            return false
        }

        val diagnostic = parseCharacter(expression).diagnostic
        if (diagnostic != null) {
            return reportError(diagnostic)
        }
        return false
    }

    private fun reportError(diagnostic: Diagnostic): Boolean {
        if (!checkOnlyErrorsThatDependOnExpectedType ||
            diagnostic.factory in errorsThatDependOnExpectedType
        ) {
            trace.report(diagnostic)
            return true
        }
        return false
    }

    private fun reportConstantExpectedTypeMismatch(
        expression: CjConstantExpression,
        typeName: String,
        expectedType: CangJieType,
        expressionType: CangJieType?
    ): Boolean {
        trace.report(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, typeName, expectedType))
        return true
    }

    /**
     * 字符解析结果，包含值或诊断错误
     */
    data class CharacterWithDiagnostic(
        val diagnostic: Diagnostic? = null,
        val value: Char? = null
    ) {
        constructor(diagnostic: Diagnostic) : this(diagnostic, null)
        constructor(value: Char) : this(null, value)
    }

    companion object {
        private val errorsThatDependOnExpectedType: Set<DiagnosticFactory<*>> =
            setOf(CONSTANT_EXPECTED_TYPE_MISMATCH)

        /**
         * 解析 Rune 字符
         */
        @JvmStatic
        fun parseRune(expression: CjConstantExpression): Char? =
            parseCharacter(expression).value

        private fun noExpectedTypeOrError(expectedType: CangJieType): Boolean =
            TypeUtils.noExpectedType(expectedType) || expectedType.isError

        private fun createErrorCharacter(diagnostic: Diagnostic): CharacterWithDiagnostic =
            CharacterWithDiagnostic(diagnostic)

        /**
         * 解析字符字面量
         */
        @JvmStatic
        fun parseCharacter(expression: CjConstantExpression): CharacterWithDiagnostic {
            val text = expression.text

            // 检查 r' 或 r" 格式
            if (text.length < 3 ||
                !(text.startsWith("r'") && text.endsWith("'") ||
                        text.startsWith("r\"") && text.endsWith("\""))
            ) {
                return createErrorCharacter(INCORRECT_CHARACTER_LITERAL.on(expression))
            }

            // 去掉前缀和引号
            val content = text.substring(2, text.length - 1)

            if (content.isEmpty()) {
                return createErrorCharacter(EMPTY_CHARACTER_LITERAL.on(expression))
            }

            // 非转义字符
            if (content[0] != '\\') {
                return if (content.length == 1) {
                    CharacterWithDiagnostic(content[0])
                } else {
                    createErrorCharacter(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, content))
                }
            }

            return escapedStringToCharacter(content, expression)
        }

        /**
         * 解析转义字符
         */
        @JvmStatic
        fun escapedStringToCharacter(text: String, expression: CjElement): CharacterWithDiagnostic {
            require(text.isNotEmpty() && text[0] == '\\') {
                "Only escaped sequences must be passed to this routine: $text"
            }

            val escape = text.substring(1) // 去掉反斜杠

            return when (escape.length) {
                0 -> illegalEscape(expression) // 裸反斜杠

                1 -> { // 单字符转义
                    val escaped = translateEscape(escape[0])
                    if (escaped != null) {
                        CharacterWithDiagnostic(escaped)
                    } else {
                        illegalEscape(expression)
                    }
                }

                5 -> { // Unicode 转义
                    if (escape[0] == 'u') {
                        try {
                            val intValue = escape.substring(1).toInt(16)
                            CharacterWithDiagnostic(intValue.toChar())
                        } catch (e: NumberFormatException) {
                            illegalEscape(expression)
                        }
                    } else {
                        illegalEscape(expression)
                    }
                }

                else -> illegalEscape(expression)
            }
        }

        private fun illegalEscape(expression: CjElement): CharacterWithDiagnostic =
            createErrorCharacter(ILLEGAL_ESCAPE.on(expression, expression))

        /**
         * 翻译转义字符
         */
        private fun translateEscape(c: Char): Char? = when (c) {
            't' -> '\t'
            'b' -> '\b'
            'n' -> '\n'
            'r' -> '\r'
            '\'' -> '\''
            '\"' -> '\"'
            '\\' -> '\\'
            '$' -> '$'
            else -> null
        }
    }
}
