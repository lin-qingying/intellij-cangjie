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
import org.cangnova.cangjie.diagnostics.PositioningStrategies.DECLARATION_NAME
import org.cangnova.cangjie.diagnostics.rendering.DeclarationWithDiagnosticComponents
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.types.CangJieType

// ========================================
// 继承和重写相关错误
// ========================================


/**
 * 抽象成员未实现
 */

val ABSTRACT_MEMBER_NOT_IMPLEMENTED: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 抽象类成员未实现
 */

val ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 多个实现成员未实现
 */

val MANY_IMPL_MEMBER_NOT_IMPLEMENTED: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 多个接口成员未实现
 */

val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED: DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 重写 final 成员
 */

val OVERRIDING_FINAL_MEMBER: DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, DeclarationDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER)

/**
 * 委托重写 final 成员
 */

val OVERRIDING_FINAL_MEMBER_BY_DELEGATION: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 没有可重写的内容
 */

val NOTHING_TO_OVERRIDE: DiagnosticFactory1<CjModifierListOwner, CallableMemberDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER)

/**
 * redef 没有可重写的内容
 */

val REDEF_NOTHING_TO_OVERRIDE: DiagnosticFactory1<CjModifierListOwner, CallableMemberDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER)

/**
 * 无法重写不可见的成员
 */

val CANNOT_OVERRIDE_INVISIBLE_MEMBER: DiagnosticFactory2<CjModifierListOwner, CallableMemberDescriptor, CallableDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER)

/**
 * 无法更改访问权限
 */

val CANNOT_CHANGE_ACCESS_PRIVILEGE: DiagnosticFactory3<CjModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.VISIBILITY_MODIFIER)

/**
 * 无法削弱访问权限
 */

val CANNOT_WEAKEN_ACCESS_PRIVILEGE: DiagnosticFactory3<CjModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor> =
    DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.VISIBILITY_MODIFIER)

/**
 * 重写时返回类型不匹配
 */

val RETURN_TYPE_MISMATCH_ON_OVERRIDE: DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, DeclarationWithDiagnosticComponents> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_RETURN_TYPE)

/**
 * 继承时返回类型不匹配
 */

val RETURN_TYPE_MISMATCH_ON_INHERITANCE: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 委托时返回类型不匹配
 */

val RETURN_TYPE_MISMATCH_BY_DELEGATION: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 重写时属性类型不匹配
 */

val PROPERTY_TYPE_MISMATCH_ON_OVERRIDE: DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_RETURN_TYPE)

/**
 * 继承时属性类型不匹配
 */

val PROPERTY_TYPE_MISMATCH_ON_INHERITANCE: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 委托时属性类型不匹配
 */

val PROPERTY_TYPE_MISMATCH_BY_DELEGATION: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 重写时 var 类型不匹配
 */

val VAR_TYPE_MISMATCH_ON_OVERRIDE: DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_RETURN_TYPE)

/**
 * 继承时 var 类型不匹配
 */

val VAR_TYPE_MISMATCH_ON_INHERITANCE: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * let 被 var 重写
 */

val LET_OVERRIDDEN_BY_VAR: DiagnosticFactory2<CjNamedDeclaration, PropertyDescriptor, PropertyDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.LET_OR_VAR_NODE)

/**
 * var 被 let 重写
 */

val VAR_OVERRIDDEN_BY_LET: DiagnosticFactory2<CjNamedDeclaration, PropertyDescriptor, PropertyDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.LET_OR_VAR_NODE)

/**
 * 委托时 var 被 let 重写
 */

val VAR_OVERRIDDEN_BY_LET_BY_DELEGATION: DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 重写静态成员错误
 */

val OVERRIDE_STATIC_ERROR: DiagnosticFactory1<PsiElement, CjNamedDeclaration> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * redef 实例成员错误
 */

val REDEF_INSTANCE_ERROR: DiagnosticFactory1<PsiElement, CjNamedDeclaration> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 重写时默认值不允许
 */

val DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE: DiagnosticFactory0<CjParameter> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.PARAMETER_DEFAULT_VALUE)

/**
 * 从超类型继承的多个默认值
 */

val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES: DiagnosticFactory1<CjParameter, ValueParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 从超类型继承的多个默认值（无显式重写）
 */

val MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_MATCH_NO_EXPLICIT_OVERRIDE: DiagnosticFactory1<CjTypeStatement, ValueParameterDescriptor> =
    DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * 冲突的继承成员
 */

val CONFLICTING_INHERITED_MEMBERS: DiagnosticFactory2<CjTypeStatement, InheritableDescriptor, Collection<CallableMemberDescriptor>> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_NAME)

/**
 * extend 成员不允许遮蔽原始类型的成员
 * 参数1: extend 成员名称
 * 参数2: 被扩展的类的描述符
 */

val EXTEND_MEMBER_CANNOT_SHADOW: DiagnosticFactory2<CjDeclaration, String, ClassifierDescriptor> =
    DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.FOR_REDECLARATION)

// ========================================
// 超类型相关错误
// ========================================

/**
 * 超类型出现两次
 */

val SUPERTYPE_APPEARS_TWICE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 超类型不是类或接口
 */

val SUPERTYPE_NOT_A_CLASS_OR_INTERFACE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 超类型列表中有多个类
 */

val MANY_CLASSES_IN_SUPERTYPE_LIST: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 超类型中的结构体
 */

val STRUCT_IN_SUPERTYPE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 超类型中的枚举
 */

val ENUM_IN_SUPERTYPE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 枚举的超类型中的类
 */

val CLASS_IN_SUPERTYPE_FOR_ENUM: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 接口有超类
 */

val INTERFACE_WITH_SUPERCLASS: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 结构体有超类
 */

val STRUCT_WITH_SUPERCLASS: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 扩展有超类
 */

val EXTEND_WITH_SUPERCLASS: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * final 超类型
 */

val FINAL_SUPERTYPE: DiagnosticFactory1<CjTypeReference, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 扩展不能接口
 */

val EXTEND_CANNOT_INTERFACE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 动态超类型
 */

val DYNAMIC_SUPERTYPE: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 动态类型不允许
 */

val DYNAMIC_NOT_ALLOWED: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 超类型未初始化
 */

val SUPERTYPE_NOT_INITIALIZED: DiagnosticFactory1<CjSuperTypeEntry, CangJieType> =
    DiagnosticFactory1.create(Severity.ERROR)

/**
 * 循环继承层次
 */

val CYCLIC_INHERITANCE_HIERARCHY: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 缺少依赖超类
 */

val MISSING_DEPENDENCY_SUPERCLASS: DiagnosticFactory2<PsiElement, FqName, FqName> =
    DiagnosticFactory2.create(Severity.ERROR)

/**
 * 密封抽象类
 */

val SEALED_ABSTRACT: DiagnosticFactory0<PsiElement> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 立即超类型参数中的投影
 */

val PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE: DiagnosticFactory0<CjTypeProjection> =
    DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.VARIANCE_IN_PROJECTION)

/**
 * 重复边界
 */

val REPEATED_BOUND: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)

/**
 * 只允许一个类边界
 */

val ONLY_ONE_CLASS_BOUND_ALLOWED: DiagnosticFactory0<CjTypeReference> =
    DiagnosticFactory0.create(Severity.ERROR)
