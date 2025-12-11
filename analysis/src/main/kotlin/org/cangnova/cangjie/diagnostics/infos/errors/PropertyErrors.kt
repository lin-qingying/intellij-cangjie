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
import org.cangnova.cangjie.psi.*

import org.cangnova.cangjie.types.CangJieType

// ========================================
// 属性和变量相关错误
// ========================================

/**
 * 必须初始化
 */
@JvmField
val MUST_BE_INITIALIZED: DiagnosticFactory0<CjVariable> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 必须初始化或是抽象的
 */
@JvmField
val MUST_BE_INITIALIZED_OR_BE_ABSTRACT: DiagnosticFactory0<CjVariable> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 必须初始化或是 final
 */
@JvmField
val MUST_BE_INITIALIZED_OR_BE_FINAL: DiagnosticFactory0<CjVariable> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 必须初始化或是 final 或抽象的
 */
@JvmField
val MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT: DiagnosticFactory0<CjVariable> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * let 重新赋值
 */
@JvmField
val LET_REASSIGNMENT: DiagnosticFactory1<CjExpression, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 预期变量
 */
@JvmField
val VARIABLE_EXPECTED: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 赋值类型不匹配
 */
@JvmField
val ASSIGNMENT_TYPE_MISMATCH: DiagnosticFactory1<CjBinaryExpression, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 表达式上下文中的赋值
 */
@JvmField
val ASSIGNMENT_IN_EXPRESSION_CONTEXT: DiagnosticFactory0<CjBinaryExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 赋值运算符模糊
 */
//@JvmField
//val ASSIGN_OPERATOR_AMBIGUITY: DiagnosticFactory1<PsiElement, Collection<out ResolvedCall<*>>> =
//    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 接口中的变量初始化器
 */
@JvmField
val VARIABLE_INITIALIZER_IN_INTERFACE: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 预期变量初始化器
 */
@JvmField
val EXPECTED_VARIABLE_INITIALIZER: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 未初始化的变量
 */
@JvmField
val UNINITIALIZED_VARIABLE: DiagnosticFactory1<CjSimpleNameExpression, VariableDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 未初始化的参数
 */
@JvmField
val UNINITIALIZED_PARAMETER: DiagnosticFactory1<CjSimpleNameExpression, ValueParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 未初始化的枚举条目
 */
@JvmField
val UNINITIALIZED_ENUM_ENTRY: DiagnosticFactory1<CjSimpleNameExpression, ClassDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 声明前初始化
 */
@JvmField
val INITIALIZATION_BEFORE_DECLARATION: DiagnosticFactory1<CjExpression, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 捕获的成员 let 初始化
 */
@JvmField
val CAPTURED_MEMBER_LET_INITIALIZATION: DiagnosticFactory1<CjExpression, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 捕获的 let 初始化
 */
@JvmField
val CAPTURED_LET_INITIALIZATION: DiagnosticFactory1<CjExpression, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不可变函数实例成员修改
 */
@JvmField
val IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION: DiagnosticFactory1<CjElement, VariableDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 本地扩展变量
 */
@JvmField
val LOCAL_EXTENSION_VARIABLE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 带有 backing field 的上下文接收者
 */
@JvmField
val CONTEXT_RECEIVERS_WITH_BACKING_FIELD: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 带有 backing field 的扩展变量
 */
@JvmField
val EXTENSION_VARIABLE_WITH_BACKING_FIELD: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 显式 backing field 不支持
 */
@JvmField
val EXPLICIT_BACKING_FIELDS_UNSUPPORTED: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 属性访问器相关错误
// ========================================

/**
 * 抽象属性带有 getter
 */
@JvmField
val ABSTRACT_PROPERTY_WITH_GETTER: DiagnosticFactory0<CjPropertyAccessor> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 抽象属性带有 setter
 */
@JvmField
val ABSTRACT_PROPERTY_WITH_SETTER: DiagnosticFactory0<CjPropertyAccessor> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 抽象属性带有初始化器
 */
@JvmField
val ABSTRACT_PROPERTY_WITH_INITIALIZER: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 非抽象类中的抽象属性
 */
@JvmField
val ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS: DiagnosticFactory2<CjModifierListOwner, String, ClassDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER)

/**
 * 主构造函数参数中的抽象属性
 */
@JvmField
val ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS: DiagnosticFactory0<CjModifierListOwner> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER)

/**
 * getter 返回类型错误
 */
@JvmField
val WRONG_GETTER_RETURN_TYPE: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * setter 返回类型错误
 */
@JvmField
val WRONG_SETTER_RETURN_TYPE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * setter 参数类型错误
 */
@JvmField
val WRONG_SETTER_PARAMETER_TYPE: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * setter 参数带有默认值
 */
@JvmField
val SETTER_PARAMETER_WITH_DEFAULT_VALUE: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * let 带有 setter
 */
@JvmField
val LET_WITH_SETTER: DiagnosticFactory0<CjPropertyAccessor> =
    DiagnosticFactory0.create(Severity.ERROR)
