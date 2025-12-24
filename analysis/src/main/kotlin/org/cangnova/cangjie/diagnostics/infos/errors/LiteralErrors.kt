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

@file:Suppress("unused")
@file:DiagnosticHolder

package org.cangnova.cangjie.diagnostics.infos.errors

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 字面量相关错误
// ========================================

/**
 * 整数字面量超出范围
 */

val INT_LITERAL_OUT_OF_RANGE: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 整数字面量按类型超出范围
 */

val INT_LITERAL_OUT_OF_RANGE_BY_TYPE: DiagnosticFactory2<CjConstantExpression, Long, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 浮点字面量超出范围
 */

val FLOAT_LITERAL_OUT_OF_RANGE: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 错误的字符字面量
 */

val INCORRECT_CHARACTER_LITERAL: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 空字符字面量
 */

val EMPTY_CHARACTER_LITERAL: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 字符字面量中字符过多
 */

val TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL: DiagnosticFactory1<CjConstantExpression, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 非法转义
 */

val ILLEGAL_ESCAPE: DiagnosticFactory1<CjElement, CjElement> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.CUT_CHAR_QUOTES)

/**
 * 非法下划线
 */

val ILLEGAL_UNDERSCORE: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 类路径上没有无符号字面量声明
 */

val UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 下划线保留
 */

val UNDERSCORE_IS_RESERVED: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 有符号常量转换为无符号
 */

val SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 多行字符串没有换行符
 */

val NO_MULTILINE_NEWLINE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)
