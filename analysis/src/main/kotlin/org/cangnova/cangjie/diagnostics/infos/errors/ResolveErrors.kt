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
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.tower.WrongResolutionToClassifier
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 解析和引用相关错误
// ========================================

/**
 * 未解析的引用
 */
@JvmField
val UNRESOLVED_REFERENCE: DiagnosticFactory1<CjReferenceExpression, CjReferenceExpression> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.FOR_UNRESOLVED_REFERENCE)

/**
 * 未解析的引用（接收者错误）
 */
@JvmField
val UNRESOLVED_REFERENCE_WRONG_RECEIVER: DiagnosticFactory1<PsiElement, Collection<ResolvedCall<*>>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不可见的引用
 */
@JvmField
val INVISIBLE_REFERENCE: DiagnosticFactory3<CjSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 不可见的引用（重导出）
 */
@JvmField
val INVISIBLE_REFERENCE_REEXPORT: DiagnosticFactory3<CjSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, FqName> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 不可见的成员
 */
@JvmField
val INVISIBLE_MEMBER: DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.CALL_ELEMENT)

/**
 * 不可见的 setter
 */
@JvmField
val INVISIBLE_SETTER: DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 无适用的候选
 */
@JvmField
val NONE_APPLICABLE: DiagnosticFactory1<PsiElement, Collection<ResolvedCall<*>>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 无法完成解析
 */
@JvmField
val CANNOT_COMPLETE_RESOLVE: DiagnosticFactory1<PsiElement, Collection<ResolvedCall<*>>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 重载解析模糊
 */
@JvmField
val OVERLOAD_RESOLUTION_AMBIGUITY: DiagnosticFactory1<PsiElement, Collection<ResolvedCall<*>>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 可调用引用解析模糊
 */
@JvmField
val CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY: DiagnosticFactory1<PsiElement, Collection<CallableDescriptor>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 可调用引用左侧不是类
 */
@JvmField
val CALLABLE_REFERENCE_LHS_NOT_A_CLASS: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 解析到分类器
 */
@JvmField
val RESOLUTION_TO_CLASSIFIER: DiagnosticFactory3<CjReferenceExpression, ClassifierDescriptor, WrongResolutionToClassifier, String> =
    DiagnosticFactory3.create(Severity.ERROR)
//
// ========================================
// 运算符相关错误
// ========================================

/**
 * 导入时运算符重命名
 */
@JvmField
val OPERATOR_RENAMED_ON_IMPORT: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不适用的运算符修饰符
 */
@JvmField
val INAPPLICABLE_OPERATOR_MODIFIER: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 赋值运算符应该返回 Unit
 */
@JvmField
val ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT: DiagnosticFactory2<CjSimpleNameExpression, DeclarationDescriptor, CjSimpleNameExpression> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 自增自减不应返回 Unit
 */
@JvmField
val INC_DEC_SHOULD_NOT_RETURN_UNIT: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 无效的二元运算符
 */
@JvmField
val INVALID_BINARY_OPERATOR: DiagnosticFactory1<PsiElement, InvalidBinaryData> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 无效的二元运算符（内联）
 */
@JvmField
val INLETID_BINARY_OPERATOR: DiagnosticFactory1<PsiElement, InvalidBinaryData> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 结果类型不匹配
 */
@JvmField
val RESULT_TYPE_MISMATCH: DiagnosticFactory3<CjExpression, String, CangJieType, CangJieType> =
    DiagnosticFactory3.create(Severity.ERROR)
