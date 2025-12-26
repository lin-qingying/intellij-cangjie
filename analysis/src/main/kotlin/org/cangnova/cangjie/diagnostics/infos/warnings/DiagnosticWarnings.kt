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

package org.cangnova.cangjie.diagnostics.infos.warnings

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.*

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 类型检查相关警告
// ========================================

/**
 * 类型不匹配（警告）
 */

val TYPE_MISMATCH_WARNING: DiagnosticFactory2<CjExpression, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 上界违反（警告）
 */

val UPPER_BOUND_VIOLATED_WARNING: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 隐式转换为 Any（警告）
 */

val IMPLICIT_CAST_TO_ANY: DiagnosticFactory2<CjExpression, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 预期参数类型不匹配（警告）
 */

val EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING: DiagnosticFactory1<CjParameter, CangJieType> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 不兼容的枚举比较（警告）
 */

val INCOMPATIBLE_ENUM_COMPARISON: DiagnosticFactory2<CjElement, CangJieType, CangJieType> =
    DiagnosticFactory2.create(Severity.WARNING)

// ========================================
// 类型推导相关警告
// ========================================

/**
 * 推导类型变量到可能空的交集
 */

val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION: DiagnosticFactory4<PsiElement, String, Collection<CangJieType>, String, String> =
    DiagnosticFactory4.create(Severity.WARNING)

/**
 * 推导到声明的上界
 */

val INFERRED_INTO_DECLARED_UPPER_BOUNDS: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.WARNING)

// ========================================
// 字面量相关警告
// ========================================

/**
 * 整数溢出
 */

val INTEGER_OVERFLOW: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 除以零
 */

val DIVISION_BY_ZERO: DiagnosticFactory0<CjExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 浮点字面量符合无穷大
 */

val FLOAT_LITERAL_CONFORMS_INFINITY: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 浮点字面量符合零
 */

val FLOAT_LITERAL_CONFORMS_ZERO: DiagnosticFactory0<CjConstantExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

// ========================================
// 声明相关警告
// ========================================

/**
 * 名称遮蔽（警告）
 */

val NAME_SHADOWING: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.FOR_REDECLARATION)

/**
 * 访问器参数名称遮蔽（警告）
 */

val ACCESSOR_PARAMETER_NAME_SHADOWING: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * return 不允许
 */

val RETURN_NOT_ALLOWED: DiagnosticFactory0<CjReturnExpression> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.RETURN_WITH_LABEL)

// ========================================
// 构造函数相关警告
// ========================================

/**
 * 没有构造函数（警告）
 */

val NO_CONSTRUCTOR_WARNING: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 预期枚举中的主构造函数委托调用
 */

val PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

// ========================================
// 属性和变量相关警告
// ========================================

/**
 * 未使用的变量
 */

val UNUSED_VARIABLE: DiagnosticFactory1<CjNamedDeclaration, VariableDescriptor> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 未使用的参数
 */

val UNUSED_PARAMETER: DiagnosticFactory1<CjParameter, VariableDescriptor> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 未使用的匿名参数
 */

val UNUSED_ANONYMOUS_PARAMETER: DiagnosticFactory1<CjParameter, VariableDescriptor> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

 /**
 * 未使用的更改值
 */

val UNUSED_CHANGED_VALUE: DiagnosticFactory1<CjElement, CjElement> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 参数无用的可变参数
 */

val USELESS_VARARG_ON_PARAMETER: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.WARNING)

// ========================================
// 继承相关警告
// ========================================

/**
 * 抽象类成员未实现（警告）
 */

val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 多个接口成员未实现（警告）
 */

val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 虚成员隐藏
 */

val VIRTUAL_MEMBER_HIDDEN: DiagnosticFactory4<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor, CjToken> =
    DiagnosticFactory4.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 重写时参数名称改变
 */

val PARAMETER_NAME_CHANGED_ON_OVERRIDE: DiagnosticFactory2<CjParameter, ClassDescriptor, ValueParameterDescriptor> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 超类型中相同参数的不同名称
 */

val DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES: DiagnosticFactory2<CjTypeStatement, Collection<out CallableMemberDescriptor>, Int> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 冲突的继承成员（警告）
 */

val CONFLICTING_INHERITED_MEMBERS_WARNING: DiagnosticFactory2<CjTypeStatement, InheritableDescriptor, Collection<CallableMemberDescriptor>> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

// ========================================
// Match 表达式相关警告
// ========================================

/**
 * match 中的冗余 else
 */

val REDUNDANT_ELSE_IN_MATCH: DiagnosticFactory0<CjMatchEntry> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.ELSE_ENTRY)

/**
 * match 中的重复标签
 */

val DUPLICATE_LABEL_IN_MATCH: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * match 中 else 放错位置
 */

val ELSE_MISPLACED_IN_MATCH: DiagnosticFactory0<CjMatchEntry> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.ELSE_ENTRY)

/**
 * match 中缺少 else（警告）
 */

val NO_ELSE_IN_MATCH_WARNING: DiagnosticFactory1<CjMatchExpression, List<MatchMissingCase>> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.MATCH_EXPRESSION)

/**
 * match 中无意义的 null
 */

val SENSELESS_NULL_IN_MATCH: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.WARNING)

// ========================================
// 类型相关警告
// ========================================

/**
 * super 限定符中冗余的类型参数
 */

val TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 类型参数约束放错位置
 */

val MISPLACED_TYPE_PARAMETER_CONSTRAINTS: DiagnosticFactory0<CjTypeParameter> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 未使用的类型别名参数
 */

val UNUSED_TYPEALIAS_PARAMETER: DiagnosticFactory2<CjTypeParameter, TypeParameterDescriptor, CangJieType> =
    DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.DECLARATION_NAME)

/**
 * 类型别名展开中的上界违反（警告）
 */

val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING: DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> =
    DiagnosticFactory3.create(Severity.WARNING)

// ========================================
// 可空类型相关警告
// ========================================

/**
 * 无用的可空检查
 */

val USELESS_NULLABLE_CHECK: DiagnosticFactory0<CjOptionType> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.OPTIONAL_TYPE)

/**
 * 无用的 is 检查
 */

val USELESS_IS_CHECK: DiagnosticFactory1<CjElement, Boolean> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 冗余的可选类型
 */

val REDUNDANT_OPTIONAL: DiagnosticFactory0<CjOptionType> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.OPTIONAL_TYPE)

/**
 * 无用的 elvis 运算符
 */

val USELESS_ELVIS: DiagnosticFactory1<CjBinaryExpression, CangJieType> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.USELESS_ELVIS)

/**
 * 无用的 elvis 运算符（右侧为 null）
 */

val USELESS_ELVIS_RIGHT_IS_NULL: DiagnosticFactory0<CjBinaryExpression> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.USELESS_ELVIS)

/**
 * 废弃的确定非空语法
 */

val DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL: DiagnosticFactory0<CjPostfixExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 不必要的安全调用
 */

val UNNECESSARY_SAFE_CALL: DiagnosticFactory1<PsiElement, CangJieType> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 安全调用将改变可空性
 */

val SAFE_CALL_WILL_CHANGE_NULLABILITY: DiagnosticFactory0<CjQualifiedExpression> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.CALL_ELEMENT_WITH_DOT)

// ========================================
// 修饰符相关警告
// ========================================

/**
 * 废弃的目标修饰符
 */

val DEPRECATED_MODIFIER_FOR_TARGET: DiagnosticFactory2<PsiElement, CjKeywordToken, String> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 目标的冗余修饰符
 */

val REDUNDANT_MODIFIER_FOR_TARGET: DiagnosticFactory2<PsiElement, CjKeywordToken, String> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 冗余的修饰符
 */

val REDUNDANT_MODIFIER: DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 废弃的修饰符对
 */

val DEPRECATED_MODIFIER_PAIR: DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 接口中冗余的 open
 */

val REDUNDANT_OPEN_IN_INTERFACE: DiagnosticFactory0<CjModifierListOwner> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.OPEN_MODIFIER)

// ========================================
// 导入相关警告
// ========================================

/**
 * 冲突的导入
 */

val CONFLICTING_IMPORT: DiagnosticFactory1<CjImportDirectiveItem, String> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.IMPORT_ALIAS)

// ========================================
// 标签相关警告
// ========================================

/**
 * 标签解析将改变
 */

val LABEL_RESOLVE_WILL_CHANGE: DiagnosticFactory2<CjSimpleNameExpression, String, String> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 标签名称冲突
 */

val LABEL_NAME_CLASH: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.WARNING)

// ========================================
// 可见性相关警告
// ========================================

/**
 * 从文件私有中暴露
 */

val EXPOSED_FROM_PRIVATE_IN_FILE: DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> =
    DiagnosticFactory3.create(Severity.WARNING)

// ========================================
// 表达式相关警告
// ========================================

/**
 * 不可达代码
 */

val UNREACHABLE_CODE: DiagnosticFactory2<CjElement, Set<CjElement>, Set<CjElement>> =
    DiagnosticFactory2.create(Severity.WARNING, ClassicPositioningStrategies.UNREACHABLE_CODE)

// ========================================
// 插件相关警告
// ========================================

/**
 * 插件警告
 */

val PLUGIN_WARNING: DiagnosticFactory1<PsiElement, RenderedDiagnostic<*>> =
    DiagnosticFactory1.create(Severity.WARNING)

// ========================================
// 新推导相关警告
// ========================================

/**
 * 新推导诊断
 */

val NEW_INFERENCE_DIAGNOSTIC: DiagnosticFactory1<PsiElement, String> =
    DiagnosticFactory1.create(Severity.WARNING)

// ========================================
// 弃用相关警告
// ========================================

/**
 * 弃用
 */

val DEPRECATION: DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> =
    DiagnosticFactory2.create(Severity.WARNING)

/**
 * 通过短名称弃用访问
 */

val DEPRECATED_ACCESS_BY_SHORT_NAME: DiagnosticFactory1<CjElement, DeclarationDescriptor> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 有歧义枚举条目的弃用解析
 */

val DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY: DiagnosticFactory2<PsiElement, PropertyDescriptor, ClassDescriptor> =
    DiagnosticFactory2.create(Severity.WARNING)

// ========================================
// 其他杂项警告
// ========================================

/**
 * 兼容性警告
 */

val COMPATIBILITY_WARNING: DiagnosticFactory1<CjElement, CallableDescriptor> =
    DiagnosticFactory1.create(Severity.WARNING)

/**
 * 带有同伴对象的循环作用域
 */

val CYCLIC_SCOPES_WITH_COMPANION: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 近似的局部类型将变为可变
 */

val APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE: DiagnosticFactory1<CjDeclaration, CangJieType> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 近似的局部类型将变为可空
 */

val APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE: DiagnosticFactory1<CjDeclaration, CangJieType> =
    DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.DECLARATION_SIGNATURE)

/**
 * 无效的 if 作为表达式（警告）
 */

val INLETID_IF_AS_EXPRESSION_WARNING: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.WARNING)

/**
 * 套娃可选类型
 */

val NESTING_DOLL_OPTINOTYPE: DiagnosticFactory0<CjOptionType> =
    DiagnosticFactory0.create(Severity.WARNING, PositioningStrategies.OPTIONAL_TYPE)
