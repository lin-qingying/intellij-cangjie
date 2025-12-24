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

package org.cangnova.cangjie.diagnostics.infos.deprecation

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 废弃诊断工厂（同时生成错误和警告）
// ========================================

/**
 * 类型检查器遇到递归问题（废弃警告）
 */
//
//val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM: DiagnosticFactoryForDeprecation0<CjExpression> =
//    DiagnosticFactoryForDeprecation0.create(LanguageFeature.ForbidRecursiveDelegateExpressions)
//
/**
 * 增强赋值中的递归类型问题（废弃警告）
 */
//
//val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT: DiagnosticFactoryForDeprecation0<CjExpression> =
//    DiagnosticFactoryForDeprecation0.create(LanguageFeature.ReportErrorsOnRecursiveTypeInsidePlusAssignment)

/**
 * 类型推导仅限输入类型（废弃警告）
 */
//
//val TYPE_INFERENCE_ONLY_INPUT_TYPES: DiagnosticFactoryForDeprecation1<PsiElement, TypeParameterDescriptor> =
//    DiagnosticFactoryForDeprecation1.create(LanguageFeature.StrictOnlyInputTypesChecks)

/**
 * 推导类型变量到空交集（废弃警告）
 */
//
//val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION: DiagnosticFactoryForDeprecation4<PsiElement, String, Collection<CangJieType>, String, String> =
//    DiagnosticFactoryForDeprecation4.create(LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection)

/**
 * 不可见的抽象成员（来自超类）
 */
//
//val INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER: DiagnosticFactoryForDeprecation2<CjTypeStatement, ClassDescriptor, Collection<CallableMemberDescriptor>> =
//    DiagnosticFactoryForDeprecation2.create(
//        LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses,
//        PositioningStrategies.DECLARATION_NAME
//    )

/**
 * 通过 backing field 重新赋值 let（废弃警告）
 */
//
//val LET_REASSIGNMENT_VIA_BACKING_FIELD: DiagnosticFactoryForDeprecation1<CjExpression, DeclarationDescriptor> =
//    DiagnosticFactoryForDeprecation1.create(LanguageFeature.RestrictionOfLetReassignmentViaBackingField)