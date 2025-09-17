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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution

/**
 * 具有作用域的声明描述符接口
 *
 * 该接口表示那些具有成员作用域的声明，能够包含其他声明作为成员。
 * 这是一个功能性接口，可以被任何需要作用域管理的描述符实现。
 *
 * 主要实现者：
 * - ClassDescriptor: 类、接口、结构体的作用域
 * - EnumDescriptor: 枚举的作用域
 * - ExtendDescriptor: 扩展的作用域
 * - PackageFragmentDescriptor: 包片段的作用域
 * - ModuleDescriptor: 模块的作用域
 *
 * 设计原则：
 * - 作用域功能与类型系统分离
 * - 作用域功能与继承机制分离
 * - 支持类型参数替换的作用域
 */
interface HasScopeDescriptor : DeclarationDescriptor {
    
    /**
     * 未进行类型替换时的成员作用域（原始作用域）
     * 
     * 这是声明的基础作用域，包含所有直接声明的成员。
     * 对于泛型类型，这个作用域中的成员可能包含未实例化的类型参数。
     */
    val unsubstitutedMemberScope: MemberScope
    
    /**
     * 根据指定的类型实参列表返回对应的成员作用域
     *
     * 当声明具有类型参数时，这个方法允许获取特定类型实参下的成员作用域。
     * 例如：List<String> 的作用域与 List<Int> 的作用域可能有所不同。
     *
     * @param typeArguments 类型实参列表
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope
    
    /**
     * 根据指定的类型替换规则返回对应的成员作用域
     *
     * 这是更通用的类型替换方法，允许复杂的类型映射。
     * 通常用于处理继承、类型别名展开等场景。
     *
     * @param typeSubstitution 类型替换映射
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope
    
    /**
     * 实例级成员作用域
     * 
     * 包含可以通过实例访问的成员（非静态成员）。
     * 默认返回空作用域，具体实现可以覆盖此属性。
     */
    val instanceScope: MemberScope
        get() = MemberScope.Empty
    
    /**
     * 静态成员作用域
     * 
     * 包含静态成员、伴生对象成员等不需要实例即可访问的成员。
     */
    val staticScope: MemberScope
}