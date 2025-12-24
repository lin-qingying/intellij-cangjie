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
// 声明相关错误
// ========================================

/**
 * 重复声明
 */

val REDECLARATION: DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.FOR_REDECLARATION)

/**
 * 枚举重复声明
 */

val ENUM_REDECLARATION: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 包或分类器重复声明
 */

val PACKAGE_OR_CLASSIFIER_REDECLARATION: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.FOR_REDECLARATION)

/**
 * 冲突的重载
 */

val CONFLICTING_OVERLOADS: DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

/**
 * 冲突的静态成员
 */

val CONFLICTING_STATIC: DiagnosticFactory2<PsiElement, Collection<DeclarationDescriptor>, String> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

/**
 * 非法上下文中的声明
 */

val DECLARATION_IN_ILLEGAL_CONTEXT: DiagnosticFactory0<CjDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 匿名函数带有名称
 */

val ANONYMOUS_FUNCTION_WITH_NAME: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 匿名函数参数带有默认值
 */

val ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.PARAMETER_DEFAULT_VALUE)

/**
 * 函数声明没有名称
 */

val FUNCTION_DECLARATION_WITH_NO_NAME: DiagnosticFactory0<CjFunction> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 没有类型没有初始化器的变量
 */

val VARIABLE_WITH_NO_TYPE_NO_INITIALIZER: DiagnosticFactory0<CjVariableDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 不能推导可见性
 */

val CANNOT_INFER_VISIBILITY: DiagnosticFactory1<CjDeclaration, CallableMemberDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

// ========================================
// 函数和方法相关错误
// ========================================

/**
 * 块体函数没有返回值
 */

val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY: DiagnosticFactory0<CjDeclarationWithBody> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_WITH_BODY)

/**
 * 块体函数没有返回值（迁移）
 */

val NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION: DiagnosticFactory0<CjDeclarationWithBody> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_WITH_BODY)

/**
 * 表达式体函数中的 return
 */

val RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY: DiagnosticFactory0<CjReturnExpression> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.RETURN_WITH_LABEL)

/**
 * 非抽象类中的抽象函数
 */

val ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS: DiagnosticFactory2<CjFunction, String, ClassDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER)

/**
 * 抽象函数没有返回类型
 */

val ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 抽象函数带有函数体
 */

val ABSTRACT_FUNCTION_WITH_BODY: DiagnosticFactory1<CjFunction, SimpleFunctionDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER)

/**
 * 抽象成员可见性错误
 */

val ABSTRACT_MEMBER_VISIBILITY_ERROR: DiagnosticFactory3<PsiElement, Modality, DescriptorKind, List<DescriptorVisibility>> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 私有函数没有函数体
 */

val PRIVATE_FUNCTION_WITH_NO_BODY: DiagnosticFactory1<CjFunction, SimpleFunctionDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.PRIVATE_MODIFIER)

/**
 * 预期需要函数调用
 */

val FUNCTION_EXPECTED: DiagnosticFactory2<CjExpression, CjExpression, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 函数调用预期
 */

val FUNCTION_CALL_EXPECTED: DiagnosticFactory2<CjExpression, CjExpression, Boolean> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.CALL_EXPRESSION)

/**
 * 常量成员函数需要常量构造函数
 */

val CONST_MEMBER_FUNCTION_REQUIRES_CONST_CONSTRUCTOR: DiagnosticFactory0<CjFunction> =
    DiagnosticFactory0.create(Severity.ERROR)
