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
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Pattern
import kotlin.reflect.KProperty

fun <T : PsiElement> create0(severity: Severity) = object {
    operator fun getValue(thisRef: DiagnosticFactory0<T>, property: KProperty<*>) =  DiagnosticFactory0.create<T>(severity)
}

// ========================================
// Match 表达式相关错误
// ========================================

/**
 * match 中条件无参数时使用逗号
 */
val COMMA_IN_MATCH_CONDITION_WITHOUT_ARGUMENT: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)
 /**
 * 非枚举条目值
 */

val NOT_ENUM_ENTRY_VALUE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 非枚举 match
 */

val NOT_ENUM_MATCH: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 枚举构造函数不匹配
 */

val ENUM_CONSTRUCTOR_MISMATCH: DiagnosticFactory1<PsiElement, Int> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * match 中缺少 else
 */

val NO_ELSE_IN_MATCH: DiagnosticFactory1<CjMatchExpression, List<MatchMissingCase>> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.MATCH_EXPRESSION)

/**
 * 按模式 match 中缺少 else
 */

val NO_ELSE_IN_MATCH_BY_PATTERN: DiagnosticFactory1<CjMatchExpression, List<Pattern>> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.MATCH_EXPRESSION)

/**
 * 无 else 的 match 需要预期类型
 */

val EXPECT_TYPE_IN_MATCH_WITHOUT_ELSE: DiagnosticFactory1<CjMatchExpression, String> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.MATCH_EXPRESSION)

/**
 * 变量引入冲突
 */

val VARIABLE_INTRODUCTION_CONFLICT: DiagnosticFactory0<CjCasePattern> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 非枚举参数构造函数
 */

val NOT_ENUM_PARAMETER_CONSTRUCTOR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 枚举条目构造函数必需
 */

val ENUM_ENTRY_CONSTRUCTOR_REQUIER: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * let 表达式无类型模式
 */

val LET_EXPRESSION_NO_TYPE_PATTERN: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不可反驳的 for-in 模式错误
 */

val IRREFUTABLE_PATTERN_FOR_IN_ERROR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不可反驳的模式错误
 */

val IRREFUTABLE_PATTERN_ERROR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// Super/This 相关错误
// ========================================

/**
 * super 不是表达式
 */

val SUPER_IS_NOT_AN_EXPRESSION: DiagnosticFactory1<CjSuperExpression, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * super 不可用
 */

val SUPER_NOT_AVAILABLE: DiagnosticFactory0<CjSuperExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不是超类型
 */

val NOT_A_SUPERTYPE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 限定超类型被其他超类型扩展
 */

val QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE: DiagnosticFactory1<CjTypeReference, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 模糊的 super
 */

val AMBIGUOUS_SUPER: DiagnosticFactory0<CjSuperExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 从接口无法访问超类
 */

val SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE: DiagnosticFactory0<CjSuperExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 没有 this
 */

val NO_THIS: DiagnosticFactory0<CjThisExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 无效的 this 类型
 */

val INVALID_THIS_TYPE: DiagnosticFactory0<CjThisType> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 无效的 this 类型（内联）
 */

val INLETID_THIS_TYPE: DiagnosticFactory0<CjThisType> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 迭代器相关错误
// ========================================

/**
 * 缺少迭代器
 */

val ITERATOR_MISSING: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 迭代器模糊
 */

val ITERATOR_AMBIGUITY: DiagnosticFactory1<PsiElement, Collection<  ResolvedCall<*>>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 可空类型上的迭代器
 */

val ITERATOR_ON_NULLABLE: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 缺少组件函数
 */

val COMPONENT_FUNCTION_MISSING: DiagnosticFactory2<CjExpression, Name, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DEFAULT)

/**
 * 组件函数模糊
 */

val COMPONENT_FUNCTION_AMBIGUITY: DiagnosticFactory2<CjExpression, Name, Collection<  ResolvedCall<*>>> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DEFAULT)

/**
 * 可空类型上的组件函数
 */

val COMPONENT_FUNCTION_ON_NULLABLE: DiagnosticFactory1<CjExpression, Name> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DEFAULT)

// ========================================
// 标签相关错误
// ========================================

/**
 * 模糊的标签
 */

val AMBIGUOUS_LABEL: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不是循环标签
 */

val NOT_A_LOOP_LABEL: DiagnosticFactory1<CjExpressionWithLabel, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * break/continue 在循环外
 */

val BREAK_OR_CONTINUE_OUTSIDE_A_LOOP: DiagnosticFactory0<CjExpressionWithLabel> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * break/continue 在 when 中
 */

val BREAK_OR_CONTINUE_IN_WHEN: DiagnosticFactory0<CjExpressionWithLabel> =
    DiagnosticFactory0.create(Severity.ERROR)
