/*
 * Copyright 2024 LinQingYing. and contributors.
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

//package com.linqingying.cangjie.descriptors
//
//import com.linqingying.cangjie.descriptors.PositioningStrategies.VARIANCE_MODIFIER
//import com.linqingying.cangjie.diagnostics.DiagnosticFactory0
//import com.linqingying.cangjie.diagnostics.DiagnosticFactory1
//import com.linqingying.cangjie.diagnostics.DiagnosticFactory2
//import com.linqingying.cangjie.diagnostics.Severity
//import com.linqingying.cangjie.psi.*
//import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
//import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategyImpl
//import com.linqingying.cangjie.types.CangJieType
//import com.intellij.psi.PsiElement
//import com.squareup.wire.internal.JvmField
//
//object Errors {
//    val TYPE_PARAMETER_ON_LHS_OF_DOT =
//        DiagnosticFactory1.create<CjSimpleNameExpression, TypeParameterDescriptor>(
//            Severity.ERROR
//        )
//
//    @JvmField
//    val EXCEPTION_FROM_ANALYZER: DiagnosticFactory1<PsiElement?, Throwable?> =
//        DiagnosticFactory1.create<PsiElement, Throwable>(Severity.ERROR)
//
////    @JvmField
////    val TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM: DiagnosticFactoryForDeprecation0< CjExpression?>? =
////        DiagnosticFactoryForDeprecation0.create< CjExpression>(LanguageFeature.ForbidRecursiveDelegateExpressions)
//
//    // Type inference
//    @JvmField
//    val CANNOT_INFER_PARAMETER_TYPE: DiagnosticFactory0<CjParameter> =
//        DiagnosticFactory0.create<CjParameter>(Severity.ERROR)
//
//    @JvmField
//    val VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION: DiagnosticFactory0<CjParameter> =
//        DiagnosticFactory0.create<CjParameter>(Severity.ERROR)
//
//    @JvmField
//    val EXPECTED_PARAMETER_TYPE_MISMATCH =
//        DiagnosticFactory1.create<CjParameter, CangJieType>(
//            Severity.ERROR
//        )
//
//    @JvmField
//    val MODIFIER_LIST_NOT_ALLOWED =
//        DiagnosticFactory0.create<CjModifierList>(Severity.ERROR)
//
//    @JvmField
//
//    val NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES =
//        DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
//
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    // Meta-errors: unsupported features, failure
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    @JvmField
//
//    val UNSUPPORTED =
//        DiagnosticFactory1.create<PsiElement, String>(Severity.ERROR)
//
//
//    @JvmField
//    val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED: DiagnosticFactory0<CjTypeParameter> =
//        DiagnosticFactory0.create<CjTypeParameter>(
//            Severity.ERROR,
//            VARIANCE_MODIFIER
//        )
//
//
//    val FUNCTION_DECLARATION_WITH_NO_NAME =
//        DiagnosticFactory0.create<CjFunction>(
//            Severity.ERROR,
//            PositioningStrategies.DECLARATION_SIGNATURE
//        )
//
//    @JvmField
//
//    val TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE =
//        DiagnosticFactory0.create<PsiElement>(Severity.ERROR)
//
//    @JvmStatic
//    val ACCESSOR_PARAMETER_NAME_SHADOWING =
//        DiagnosticFactory0.create<PsiElement>(Severity.WARNING)
//
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    // Errors in declarations
//    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    // Imports
//    val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON =
//        DiagnosticFactory1.create<CjSimpleNameExpression, ClassDescriptor>(
//            Severity.ERROR
//        )
//
//    @JvmField
//
//    val UNRESOLVED_REFERENCE_WRONG_RECEIVER =
//        DiagnosticFactory1.create<PsiElement, Collection< out ResolvedCall<*>>>(
//            Severity.ERROR
//        )
//
//    @JvmField
////    @get:JvmName("UNRESOLVED_REFERENCE")
//    val UNRESOLVED_REFERENCE =
//        DiagnosticFactory1.create<CjReferenceExpression, CjReferenceExpression>(
//            Severity.ERROR,
//            PositioningStrategies.FOR_UNRESOLVED_REFERENCE
//        )
//
//    @JvmField
//    val NO_RECEIVER_ALLOWED =
//        DiagnosticFactory0.create<CjExpression>(Severity.ERROR)
//
//    @JvmField
//    val FUNCTION_EXPECTED =
//        DiagnosticFactory2.create<CjExpression, CjExpression, CangJieType>(
//            Severity.ERROR
//        )
//
//}
//
