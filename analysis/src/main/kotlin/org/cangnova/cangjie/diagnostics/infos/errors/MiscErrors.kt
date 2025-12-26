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
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.*

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 命名参数目标类型枚举
// ========================================

/**
 * 命名参数不允许的目标类型
 *
 * 标识在哪些场景下不允许使用命名参数。
 */
enum class BadNamedArgumentsTarget {
    /**
     * 非仓颉函数（如 Java 函数）
     */
    NON_CANGJIE_FUNCTION,

    /**
     * 互操作函数（如 Obj-C 函数桥接）
     */
    INTEROP_FUNCTION,

    /**
     * 函数类型的 invoke 调用
     */
    INVOKE_ON_FUNCTION_TYPE,

    /**
     * Expected 类成员
     */
    EXPECTED_CLASS_MEMBER
}

// ========================================
// 可空类型相关错误
// ========================================

/**
 * 确定非空上的可空类型
 */

val NULLABLE_ON_DEFINITELY_NOT_OPTIONAL: DiagnosticFactory0<CjOptionType> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不安全的调用
 */

val UNSAFE_CALL: DiagnosticFactory1<PsiElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不安全的隐式 invoke 调用
 */

val UNSAFE_IMPLICIT_INVOKE_CALL: DiagnosticFactory1<PsiElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 无法检查已擦除的类型
 */

val CANNOT_CHECK_FOR_ERASED: DiagnosticFactory1<CjElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

// ========================================
// 安全调用相关错误
// ========================================

/**
 * 意外的安全调用
 */

val UNEXPECTED_SAFE_CALL: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 非法选择器
 */

val ILLEGAL_SELECTOR: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 智能转换相关错误
// ========================================

/**
 * 智能转换不可能
 */

val SMARTCAST_IMPOSSIBLE: DiagnosticFactory3<CjExpression, CangJieType, String, String> =
    DiagnosticFactory3.create(Severity.ERROR)

// ========================================
// Lambda 相关错误
// ========================================

/**
 * 新行上意外的尾随 lambda
 */

val UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE: DiagnosticFactory0<CjLambdaExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 太多 lambda 表达式参数
 */

val MANY_LAMBDA_EXPRESSION_ARGUMENTS: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 展开 lambda 或可调用引用
 */

val SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE: DiagnosticFactory0<LeafPsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 元组相关错误
// ========================================

/**
 * 元组参数过少
 */

val TUPLE_ARGS_TOO_FEW: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 元组参数不匹配
 */

val TUPLE_ARGS_MISMATCH: DiagnosticFactory2<CjElement, Int, Int> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 没有元组 get 方法
 */

val NO_GET_FOR_TUPLE_METHOD: DiagnosticFactory0<CjArrayAccessExpression> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS)

/**
 * 没有元组 set 方法
 */

val NO_SET_FOR_TUPLE_METHOD: DiagnosticFactory0<CjArrayAccessExpression> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS)

/**
 * 非整数元组索引
 */

val NON_INTEGER_TUPLE_INDEX: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 元组索引越界
 */

val TUPLE_INDEX_OUT_OF_RANGE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 委托相关错误
// ========================================

/**
 * 委托特殊函数无适用候选
 */

val DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE: DiagnosticFactory2<CjExpression, String, Collection<  ResolvedCall<*>>> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 缺少委托特殊函数
 */

val DELEGATE_SPECIAL_FUNCTION_MISSING: DiagnosticFactory3<CjExpression, String, CangJieType, String> =
    DiagnosticFactory3.create(Severity.ERROR)

// ========================================
// 参数相关错误
// ========================================

/**
 * 混合命名和位置参数
 */

val MIXING_NAMED_AND_POSITIONED_ARGUMENTS: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 命名参数不允许
 */

val NAMED_ARGUMENTS_NOT_ALLOWED: DiagnosticFactory1<PsiElement, BadNamedArgumentsTarget> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 参数传递了两次
 */

val ARGUMENT_PASSED_TWICE: DiagnosticFactory0<CjReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 太多参数
 */

val TOO_MANY_ARGUMENTS: DiagnosticFactory1<PsiElement, CallableDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 参数没有值
 */

val NO_VALUE_FOR_PARAMETER: DiagnosticFactory1<CjElement, ValueParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.VALUE_ARGUMENTS)

/**
 * 命名参数后的非命名参数
 */

val NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 命名参数后的位置参数
 */

val POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 歧义参数的名称
 */

val NAME_FOR_AMBIGUOUS_PARAMETER: DiagnosticFactory0<CjReferenceExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 命名参数未找到
 */

val NAMED_PARAMETER_NOT_FOUND: DiagnosticFactory1<CjReferenceExpression, CjReferenceExpression> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.FOR_UNRESOLVED_REFERENCE)

/**
 * 括号外的可变参数
 */

val VARARG_OUTSIDE_PARENTHESES: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * catch 参数带默认值
 */

val CATCH_PARAMETER_WITH_DEFAULT_VALUE: DiagnosticFactory0<CjParameterBase> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 命名参数前缀缺失
 */

val NAMED_PARAMETER_PREFIX_MISSING: DiagnosticFactory1<CjElement, Set<Name>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 函数类型参数名称重复
 */

val DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

// ========================================
// 数组访问相关错误
// ========================================

/**
 * 没有 get 方法
 */

val NO_GET_METHOD: DiagnosticFactory0<CjArrayAccessExpression> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS)

/**
 * 没有 set 方法
 */

val NO_SET_METHOD: DiagnosticFactory0<CjArrayAccessExpression> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS)

/**
 * VArray 大小不匹配
 */

val VARRAY_SIZE_MISMATCH: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 投影相关错误
// ========================================

/**
 * 非类类型参数上的投影
 */

val PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT: DiagnosticFactory0<CjTypeProjection> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.VARIANCE_IN_PROJECTION)

/**
 * 冲突的投影
 */

val CONFLICTING_PROJECTION: DiagnosticFactory1<CjTypeProjection, ClassifierDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.VARIANCE_IN_PROJECTION)

/**
 * 成员被投影
 */

val MEMBER_PROJECTED: DiagnosticFactory2<CjElement, CallableDescriptor, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

// ========================================
// 注解相关错误
// ========================================

/**
 * 无效的注解成员类型
 */

val INVALID_TYPE_OF_ANNOTATION_MEMBER: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 可选的注解成员类型
 */

val OPTIONAL_TYPE_OF_ANNOTATION_MEMBER: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 无效的注解成员类型（内联）
 */

val INLETID_TYPE_OF_ANNOTATION_MEMBER: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 重复的注解
 */

val REPEATED_ANNOTATION: DiagnosticFactory0<CjAnnotation> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 函数类型上的非括号注解
 */

val NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 表达式相关错误
// ========================================

/**
 * 预期表达式
 */

val EXPRESSION_EXPECTED: DiagnosticFactory1<CjExpression, CjExpression> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 预期表达式但发现包
 */

val EXPRESSION_EXPECTED_PACKAGE_FOUND: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 类型参数不是表达式
 */

val TYPE_PARAMETER_IS_NOT_AN_EXPRESSION: DiagnosticFactory1<CjExpression, TypeParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 类型后预期成员或构造函数
 */

val EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE: DiagnosticFactory1<CjExpression, ClassifierDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不是类
 */

val NOT_A_CLASS: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 非 const let 在常量表达式中使用
 */

val NON_CONST_LET_USED_IN_CONSTANT_EXPRESSION: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不允许接收者
 */

val NO_RECEIVER_ALLOWED: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 可见性相关错误
// ========================================

/**
 * 暴露的类型别名展开类型
 */

val EXPOSED_TYPEALIAS_EXPANDED_TYPE: DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 预期私有声明
 */

val EXPECTED_PRIVATE_DECLARATION: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 预期带有函数体的声明
 */

val EXPECTED_DECLARATION_WITH_BODY: DiagnosticFactory0<CjDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

// ========================================
// 静态相关错误
// ========================================

/**
 * 静态实例访问
 */

val STATIC_INSTANCE_ACCESS: DiagnosticFactory1<PsiElement, PsiElement> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 静态上下文引用错误
 */

val STATIC_CONTEXT_REFERENCE_ERROR: DiagnosticFactory2<PsiElement, DescriptorKind, DeclarationDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 实例访问静态成员错误
 */

val INSTANCE_ACCESS_STATIC_MEMBER_ERROR: DiagnosticFactory2<PsiElement, DescriptorKind, DeclarationDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR)

// ========================================
// 插件相关错误
// ========================================

/**
 * 插件错误
 */

val PLUGIN_ERROR: DiagnosticFactory1<PsiElement, RenderedDiagnostic<*>> =
    DiagnosticFactory1.create(Severity.ERROR)

// ========================================
// 新推导相关错误
// ========================================

/**
 * 新推导错误
 */

val NEW_INFERENCE_ERROR: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 新推导未知错误
 */
//
val NEW_INFERENCE_UNKNOWN_ERROR: DiagnosticFactory2<PsiElement, CandidateApplicability, String> =
    DiagnosticFactory2.create(Severity.ERROR)

// ========================================
// Main 函数相关错误
// ========================================

/**
 * main 函数返回类型错误
 */

val MAIN_FUNCTION_RETURN_TYPE: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * main 函数参数数量错误
 */

val MAIN_FUNCTION_PARAMETER_COUNT: DiagnosticFactory0<CjParameterList> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * main 函数参数类型错误
 */

val MAIN_FUNCTION_PARAMETER_TYPE: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * main 函数数量错误
 */

val MAIN_FUNCTION_NUMBER_ERROR: DiagnosticFactory0<CjMainFunction> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 宏相关错误
// ========================================

/**
 * 无效的宏类型
 */

val INVALID_MACRO_TYPE: DiagnosticFactory2<PsiElement, String, CangJieType> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 过多的宏参数
 */

val EXCESSIVE_MACRO_PARAMS: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 其他杂项错误
// ========================================

/**
 * 消息错误
 */

val MESSAGE_ERROR: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不支持
 */

val UNSUPPORTED: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不支持的功能
 */

val UNSUPPORTED_FEATURE: DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * yield 是保留字
 */

val YIELD_IS_RESERVED: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 右侧无类型参数
 */

val NO_TYPE_ARGUMENTS_ON_RHS: DiagnosticFactory2<CjTypeReference, Int, String> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 枚举条目作为类型
 */

val ENUM_ENTRY_AS_TYPE: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 是枚举条目
 */

val IS_ENUM_ENTRY: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 约束中名称不是类型参数
 */

val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER: DiagnosticFactory2<CjSimpleNameExpression, CjTypeConstraint, CjTypeParameterListOwner> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 接口体中不能有变量
 */

val INTERFACE_BODY_NO_VARIABLES: DiagnosticFactory0<CjVariableDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 无操作符
 */

val NO_CALL_OPERATOR: DiagnosticFactory1<CjCallExpression, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 分析器异常
 */

val EXCEPTION_FROM_ANALYZER: DiagnosticFactory1<PsiElement, Throwable> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 缺少 stdlib
 */

val MISSING_STDLIB: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 上下文接收者之间的子类型化
 */

val SUBTYPING_BETWEEN_CONTEXT_RECEIVERS: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 编译器影响的语法错误
 */

val COMPILER_AFFECTED_SYNTAX_ERROR: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 编译器影响的语法错误（带消息）
 */

val COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 模糊匿名类型推导
 */

val AMBIGUOUS_ANONYMOUS_TYPE_INFERRED: DiagnosticFactory1<CjDeclaration, Collection<CangJieType>> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 不安全表达式错误
 */

val UNSAFE_EXPRESSION_ERROR: DiagnosticFactory0<CjParameterList> =
    DiagnosticFactory0.create(Severity.ERROR)
