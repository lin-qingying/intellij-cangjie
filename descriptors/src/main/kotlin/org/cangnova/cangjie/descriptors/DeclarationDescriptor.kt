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

import org.cangnova.cangjie.descriptors.annotations.Annotated

/**
 * 声明描述符接口
 * 该接口是所有声明描述符的基接口，提供了声明的基本信息和访问方法
 */
interface DeclarationDescriptor : Annotated,
    Named,
    ValidateableDescriptor {
    /**
     * 获取原始声明描述符
     * @return 对应于该元素原始声明的描述符
     * 通过替换类型参数（声明类或元素本身的参数）可以从原始描述符获得描述符
     * 如果当前描述符本身就是原始描述符，则返回 `this` 对象
     */
    val original: DeclarationDescriptor
    val isStatic: Boolean get() = false

    /**
     * 获取包含声明的描述符
     * @return 包含当前声明的声明描述符，如果是顶层声明则返回null
     */
    val containingDeclaration: DeclarationDescriptor?

    /**
     * 获取可见性
     * @return 声明的可见性，默认为PUBLIC
     */
    val visibility: DescriptorVisibility get() = DescriptorVisibilities.PUBLIC
    
    /**
     * 判断是否为顶层声明
     * @return 如果是顶层声明返回true，否则返回false，默认为false
     */
    val isTopLevel: Boolean get() = false
    
    /**
     * 判断是否为局部声明
     * @return 如果可见性为LOCAL则返回true，否则返回false
     */
    val isLocal get() = visibility == DescriptorVisibilities.LOCAL
    
    /**
     * 接受访问者模式的访问
     * @param visitor 访问者对象
     * @param data 传递给访问者的数据
     * @return 访问结果
     */
    fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R?

    /**
     * 接受无返回值访问者模式的访问
     * @param visitor 无返回值的访问者对象
     */
    fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>)
}
