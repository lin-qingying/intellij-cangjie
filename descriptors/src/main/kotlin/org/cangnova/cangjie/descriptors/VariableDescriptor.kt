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

import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 变量描述符接口，继承自`ValueDescriptor`和`MemberDescriptor`，用于描述变量的元信息，如编译时常量、是否为`const`或`var`等。
 */
interface VariableDescriptor : ValueDescriptor, MemberDescriptor/*,
   CallableMemberDescriptor */ {


    /**
     * 获取变量的编译时常量初始值，如果变量没有编译时常量初始值则返回`null`。
     *
     * @return 编译时常量初始值，可能为`null`
     */
    fun getCompileTimeInitializer(): ConstantValue<*>? = null

    /**
     * 清除编译时常量初始值的缓存（仅限IDE使用，编译器内部请勿调用）。
     *
     * 注意：此方法仅供IDE使用。
     */
    fun cleanCompileTimeInitializerCache() {}



    /**
     * 使用类型替换器替换当前变量的类型，返回替换后的变量描述符（可能为`null`）。
     *
     * @param substitutor 类型替换器
     * @return 替换后的变量描述符，可能为`null`
     */
    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor?
    /**
     * 检查是否为`const`变量（与`isVar`互斥）。
     *
     * @return `true`表示是`const`变量
     */
    val isConst: Boolean

    //    bool isActual();
    //
    //    bool isExternal();
    /**
     * 检查是否为`var`变量（与`isConst`互斥）。
     *
     * @return `true`表示是`var`变量
     */
    val isVar: Boolean
    /**
     * 检查是否声明了默认值。
     *
     * @return `true`表示声明了默认值
     */
    val declaresDefaultValue:Boolean get() = false
}
