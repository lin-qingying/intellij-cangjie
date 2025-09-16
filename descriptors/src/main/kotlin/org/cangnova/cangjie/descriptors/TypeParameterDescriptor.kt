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


import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.model.TypeParameterMarker

/**
 * 类型参数描述符接口，继承自`ClassifierDescriptor`和`TypeParameterMarker`，用于描述泛型类型参数的元信息，如方差、上界等。
 */
interface TypeParameterDescriptor : ClassifierDescriptor, TypeParameterMarker {
    //    boolean isReified();

    /**
     * 获取类型参数的方差（`Variance`），如协变（`out`）、逆变（`in`）或不变。
     *
     * @return 类型参数的方差
     */
    val variance: Variance
    override val superTypes: Collection<CangJieType>
        get() = error("Should not be called")

    /**
     * 获取类型参数的上界列表，返回包含所有上界类型的列表。
     *
     * @return 上界类型列表
     */
    val upperBounds: List<CangJieType>


    /**
     * 获取类型参数的构造器，返回表示该类型参数构造的`TypeConstructor`。
     *
     * @return 类型构造器
     */
    override val typeConstructor:  TypeConstructor

    /**
     * 获取原始类型参数描述符，通常是当前描述符或其覆盖的版本。
     *
     * @return 原始类型参数描述符
     */
    override val original: TypeParameterDescriptor


    /**
     * 获取类型参数在其声明列表中的索引（0基）。
     *
     * @return 类型参数的索引
     */
    val index: Int

    /**
     * 检查当前类型参数是否是从外部声明捕获的类型参数的副本（通常用于内部类的类型构造器）。
     * 如果返回`true`，则表示满足以下条件：
     * 1. 当前参数的所属声明是内部声明；
     * 2. `original`返回外部声明的原始类型参数；
     * 3. `typeConstructor`与原始声明的类型构造器一致（至少通过`equals`判断）。
     */
    /**
     * 检查当前类型参数是否是从外部声明捕获的副本。
     *
     * @return `true`表示是捕获的副本
     */
    val isCapturedFromOuterDeclaration: Boolean


    /**
     * 获取存储管理器，通常用于管理类型参数的存储或缓存。
     *
     * @return 存储管理器
     */
    val storageManager: StorageManager


    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.LOCAL
}
