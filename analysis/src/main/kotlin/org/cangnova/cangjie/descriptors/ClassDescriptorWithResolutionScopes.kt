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

import org.cangnova.cangjie.ReadOnly
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.resolve.scopes.LexicalScope

/**
 * ClassDescriptorWithResolutionScopes接口继承自ClassDescriptor，提供了与作用域解析相关的功能。
 * 该接口定义了如何获取类成员声明解析作用域、已声明的可调用成员、初始化块解析作用域、
 * 类头解析作用域和构造函数头解析作用域的方法。
 */
interface ClassDescriptorWithResolutionScopes : ClassDescriptor {
    /**
     * 获取用于成员声明解析的词法作用域。
     *
     * @return 用于成员声明解析的词法作用域。
     */
    val scopeForMemberDeclarationResolution: LexicalScope

    @get:ReadOnly
    val declaredCallableMembers: MutableCollection<CallableMemberDescriptor>

    /**
     * 获取用于初始化块解析的词法作用域。
     *
     * @return 用于初始化块解析的词法作用域。
     */
    val scopeForInitializerResolution: LexicalScope

    /**
     * 获取用于类头解析的词法作用域，类头包括类名、修饰符等。
     *
     * @return 用于类头解析的词法作用域。
     */
    val scopeForClassHeaderResolution: LexicalScope

    /**
     * 获取用于构造函数头解析的词法作用域，构造函数头包括构造函数的参数、修饰符等。
     *
     * @return 用于构造函数头解析的词法作用域。
     */
    val scopeForConstructorHeaderResolution: LexicalScope
}
