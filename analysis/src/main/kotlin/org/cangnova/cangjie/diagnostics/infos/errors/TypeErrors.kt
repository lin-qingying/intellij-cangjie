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
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 类型检查相关错误
// ========================================

/**
 * 类型不匹配
 */
@JvmField
val TYPE_MISMATCH: DiagnosticFactory2<CjExpression, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 返回类型不匹配
 */
@JvmField
val RETURN_TYPE_MISMATCH: DiagnosticFactory1<CjExpression, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 上界违反
 */
@JvmField
val UPPER_BOUND_VIOLATED: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 接收者类型不匹配
 */
@JvmField
val RECEIVER_TYPE_MISMATCH: DiagnosticFactory2<PsiElement, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 预期类型不匹配
 */
@JvmField
val EXPECTED_TYPE_MISMATCH: DiagnosticFactory1<CjExpression, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 预期参数类型不匹配
 */
@JvmField
val EXPECTED_PARAMETER_TYPE_MISMATCH: DiagnosticFactory1<CjParameter, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 常量预期类型不匹配
 */
@JvmField
val CONSTANT_EXPECTED_TYPE_MISMATCH: DiagnosticFactory2<CjConstantExpression, String, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 类型推导延迟变量在接收者类型中
 */
@JvmField
val TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 元组模式类型不匹配
 */
@JvmField
val TUPLE_PATTERN_TYPE_MISMATCH: DiagnosticFactory1<CjElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 由于函数中的 equals lambda 导致类型不匹配
 */
@JvmField
val TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN: DiagnosticFactory1<CjElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 由于类型投影导致类型不匹配
 */
@JvmField
val TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS: DiagnosticFactory1<CjElement, TypeMismatchDueToTypeProjectionsData> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 多个超类型导致类型不匹配
 */
@JvmField
val TYPE_MISMATCH_MULTIPLE_SUPERTYPES: DiagnosticFactory1<PsiElement, List<CangJieType>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不兼容的类型
 */
@JvmField
val INCOMPATIBLE_TYPES: DiagnosticFactory2<CjElement, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 不兼容的枚举比较（错误）
 */
@JvmField
val INCOMPATIBLE_ENUM_COMPARISON_ERROR: DiagnosticFactory2<CjElement, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 约束中的类型不匹配
 */
@JvmField
val TYPE_MISMATCH_IN_CONSTRAINT: DiagnosticFactory3<PsiElement, CangJieType, CangJieType, ConstraintPosition> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 约束中的上界违反
 */
@JvmField
val UPPER_BOUND_VIOLATION_IN_CONSTRAINT: DiagnosticFactory4<CjElement, Name, Name, CangJieType, CangJieType> =
    DiagnosticFactory4.create(Severity.ERROR)

/**
 * for 循环中的类型不匹配
 */
@JvmField
val TYPE_MISMATCH_IN_FOR_LOOP: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

// ========================================
// 类型推导相关错误
// ========================================

/**
 * 无法推导参数类型
 */
@JvmField
val CANNOT_INFER_PARAMETER_TYPE: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 值参数没有类型注解
 */
@JvmField
val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 新推导：参数信息不足
 */
@JvmField
val NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER: DiagnosticFactory2<PsiElement, String, DeclarationDescriptor?> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 数组字面量类型推导失败
 */
@JvmField
val ARRAY_LITERAL_TYPE_INFERENCE_FAILED: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 只能通过不受限的构建器推导推导
 */
@JvmField
val COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 隐式 Nothing 属性类型
 */
@JvmField
val IMPLICIT_NOTHING_PROPERTY_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 隐式 Nothing 返回类型
 */
@JvmField
val IMPLICIT_NOTHING_RETURN_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 隐式交集类型
 */
@JvmField
val IMPLICIT_INTERSECTION_TYPE: DiagnosticFactory1<PsiElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 缩写 Nothing 属性类型
 */
@JvmField
val ABBREVIATED_NOTHING_PROPERTY_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 缩写 Nothing 返回类型
 */
@JvmField
val ABBREVIATED_NOTHING_RETURN_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 返回类型未指定（错误）
 */
@JvmField
val RETURN_TYPE_NOT_SPECIFIED_ERROR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)