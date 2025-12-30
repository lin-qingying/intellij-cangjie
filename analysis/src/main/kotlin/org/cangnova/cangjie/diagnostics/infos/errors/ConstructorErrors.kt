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

// ========================================
// 构造函数相关错误
// ========================================

/**
 * 没有构造函数
 */

val NO_CONSTRUCTOR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 枚举类构造函数调用
 */

val ENUM_CLASS_CONSTRUCTOR_CALL: DiagnosticFactory0<CjCallExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 密封类构造函数调用
 */

val SEALED_CLASS_CONSTRUCTOR_CALL: DiagnosticFactory0<CjCallExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 需要显式委托调用
 */

val EXPLICIT_DELEGATION_CALL_REQUIRED: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

/**
 * 预期主构造函数委托调用
 */

val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

/**
 * 枚举构造函数中的委托 super 调用
 */

val DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR: DiagnosticFactory0<CjConstructorDelegationReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 循环构造函数委托调用
 */

val CYCLIC_CONSTRUCTOR_DELEGATION_CALL: DiagnosticFactory0<CjConstructorDelegationReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 主构造函数中调用 this 无效
 */

val INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR: DiagnosticFactory0<CjConstructorDelegationReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 主构造函数中调用 this 无效（内联）
 */

val INLETID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR: DiagnosticFactory0<CjConstructorDelegationReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 多个主构造函数
 */

val MULTIPLE_PRIMARY_CONSTRUCTORS: DiagnosticFactory1<CjPrimaryConstructor, ClassDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 接口中的构造函数
 */

val CONSTRUCTOR_IN_INTERFACE: DiagnosticFactory0<CjDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 构造函数名称不一致
 */

val CONSTRUCTOR_NAME_INCONSISTENCY: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 函数体中意外的终结器
 */

val UNEXPECTED_FINALIZER_IN_BODY_ERROR: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 终结器不能有参数
 */

val FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 函数体中意外的构造函数
 */

val UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)
