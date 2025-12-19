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
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 类型参数相关错误
// ========================================

/**
 * 循环泛型上界
 */
@JvmField
val CYCLIC_GENERIC_UPPER_BOUND: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * catch 子句中的类型参数
 */
@JvmField
val TYPE_PARAMETER_IN_CATCH_CLAUSE: DiagnosticFactory0<CjParameterBase> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 类型参数在点左侧
 */
@JvmField
val TYPE_PARAMETER_ON_LHS_OF_DOT: DiagnosticFactory1<CjSimpleNameExpression, TypeParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 类型参数上不允许有方差
 */
@JvmField
val VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED: DiagnosticFactory0<CjTypeParameter> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.VARIANCE_MODIFIER)

/**
 * 废弃的类型参数语法
 */
@JvmField
val DEPRECATED_TYPE_PARAMETER_SYNTAX: DiagnosticFactory0<CjTypeParameterList> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 不允许类型参数
 */
@JvmField
val TYPE_PARAMETERS_NOT_ALLOWED: DiagnosticFactory0<CjDeclaration> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE)

/**
 * 类型参数值不一致
 */
@JvmField
val INCONSISTENT_TYPE_PARAMETER_VALUES: DiagnosticFactory3<CjSuperTypeList, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 类型参数边界不一致
 */
@JvmField
val INCONSISTENT_TYPE_PARAMETER_BOUNDS: DiagnosticFactory3<CjTypeParameter, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 冲突的上界
 */
@JvmField
val CONFLICTING_UPPER_BOUNDS: DiagnosticFactory1<CjNamedDeclaration, TypeParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 如果被类型参数约束则不允许边界
 */
@JvmField
val BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER: DiagnosticFactory0<CjElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 错误的类型参数数量
 */
@JvmField
val WRONG_NUMBER_OF_TYPE_ARGUMENTS: DiagnosticFactory2<CjElement, Int, DeclarationDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 不允许类型参数
 */
@JvmField
val TYPE_ARGUMENTS_NOT_ALLOWED: DiagnosticFactory1<CjElement, String> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 类型参数不在枚举条目后
 */
@JvmField
val TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY: DiagnosticFactory2<CjElement, DeclarationDescriptor, DeclarationDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 嵌套引用时外部类的类型参数
 */
@JvmField
val TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED: DiagnosticFactory0<CjTypeArgumentList> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 类型别名相关错误
// ========================================

/**
 * 类型别名展开为格式错误的类型
 */
@JvmField
val TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE: DiagnosticFactory2<CjTypeReference, CangJieType, String> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 类型别名展开中的上界违反
 */
@JvmField
val UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION: DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR)

/**
 * 类型别名应该展开为类
 */
@JvmField
val TYPEALIAS_SHOULD_EXPAND_TO_CLASS: DiagnosticFactory1<CjTypeReference, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 递归类型别名展开
 */
@JvmField
val RECURSIVE_TYPEALIAS_EXPANSION: DiagnosticFactory1<CjElement, ClassifierDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 类型别名展开中的冲突投影
 */
@JvmField
val CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION: DiagnosticFactory1<CjElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 类型别名参数上不允许边界
 */
@JvmField
val BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 展开的类型不能被继承
 */
@JvmField
val EXPANDED_TYPE_CANNOT_BE_INHERITED: DiagnosticFactory1<CjTypeElement, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

// ========================================
// 修饰符相关错误
// ========================================

/**
 * 不兼容的修饰符
 */
@JvmField
val INCOMPATIBLE_MODIFIERS: DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 重复的修饰符
 */
@JvmField
val REPEATED_MODIFIER: DiagnosticFactory1<PsiElement, CjKeywordToken> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 错误的修饰符目标
 */
@JvmField
val WRONG_MODIFIER_TARGET: DiagnosticFactory2<PsiElement, CjKeywordToken, String> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * catch 参数上的 let/var
 */
@JvmField
val LET_OR_VAR_ON_CATCH_PARAMETER: DiagnosticFactory1<PsiElement, CjKeywordToken> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 循环参数上的 let/var
 */
@JvmField
val LET_OR_VAR_ON_LOOP_PARAMETER: DiagnosticFactory1<PsiElement, CjKeywordToken> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不允许修饰符列表
 */
@JvmField
val MODIFIER_LIST_NOT_ALLOWED: DiagnosticFactory0<CjModifierList> =
    DiagnosticFactory0.create(Severity.ERROR)

// ========================================
// 导入相关错误
// ========================================

/**
 * 包不能导入
 */
@JvmField
val PACKAGE_CANNOT_BE_IMPORTED: DiagnosticFactory0<CjSimpleNameExpression> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 模块包不能导入
 */
@JvmField
val MODULE_PACKAGE_CANNOT_BE_IMPORTED: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 模块名不能在表达式中使用
 *
 * 在仓颉语言中，模块名（如 std）只能在导入语句中使用，不能在限定表达式中使用。
 * 例如：std.core.String() 是非法的，应该先 import std.core，然后使用 core.String()
 */
@JvmField
val MODULE_CANNOT_BE_USED_IN_EXPRESSION: DiagnosticFactory1<CjSimpleNameExpression, Name> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 模块名不能作为类型使用
 *
 * 在仓颉语言中，模块名不能出现在类型位置的限定表达式中。
 * 例如：let x: std.core.String 是非法的
 */
@JvmField
val MODULE_CANNOT_BE_USED_AS_TYPE: DiagnosticFactory1<CjSimpleNameExpression, Name> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 包不能被重导出
 *
 * 在仓颉语言中，只有具体的声明可以被重导出，包本身不能被重导出。
 * 例如：public import std.core 是非法的
 */
@JvmField
val PACKAGE_CANNOT_BE_REEXPORTED: DiagnosticFactory2<CjImportDirectiveItem, FqName, DescriptorVisibility> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 不能导入
 */
@JvmField
val CANNOT_BE_IMPORTED: DiagnosticFactory1<CjSimpleNameExpression, Name> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 不能从单例全部导入
 */
@JvmField
val CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON: DiagnosticFactory1<CjSimpleNameExpression, ClassDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 自导入不允许
 */
@JvmField
val SELF_IMPORT_NOT_ALLOWED: DiagnosticFactory1<CjImportDirectiveItem, FqName> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 导入的包修改不允许
 */
@JvmField
val IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED: DiagnosticFactory2<CjImportDirectiveItem, FqName, DescriptorVisibility> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 循环导入
 */
@JvmField
val CYCLIC_IMPORT: DiagnosticFactory1<CjPackageDirective, List<FqName>> =
    DiagnosticFactory1.create(Severity.ERROR)

// ========================================
// 包相关错误
// ========================================

/**
 * 包名不一致
 */
@JvmField
val INCONSISTENT_PACKAGE_MACOR: DiagnosticFactory0<CjPackageDirective> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 包修饰符不一致
 */
@JvmField
val INCONSISTENT_PACKAGE_MODIFIERS: DiagnosticFactory1<CjPackageDirective, FqName> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 宏包名不一致
 */
@JvmField
val INCONSISTENT_MACRO_PACKAGE_NAME: DiagnosticFactory0<CjPackageDirective> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 包访问违规
 */
@JvmField
val PACKAGE_ACCESS_VIOLATION: DiagnosticFactory2<CjPackageDirective, FqName, FqName> =
    DiagnosticFactory2.create(Severity.ERROR)
