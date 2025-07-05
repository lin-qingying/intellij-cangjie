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

package cn.cangnova.cangjie.utils.slicedMap

import com.intellij.openapi.util.Key

/**
 * 将键与切片关联的抽象类
 * 
 * 这个类扩展了IntelliJ平台的Key类，并将其与切片关联起来，
 * 使得可以通过键直接访问对应的切片。这种设计允许在切片映射中
 * 更方便地管理和访问特定类型的数据。
 *
 * @param K 键的类型参数
 * @param V 值的类型参数
 * @param Slice 切片的类型参数，必须是ReadOnlySlice的子类型
 * @param debugName 用于调试的名称
 */
abstract class KeyWithSlice<K, V, out Slice : ReadOnlySlice<K, V>>(debugName: String) : Key<V>(debugName) {
    /**
     * 与此键关联的切片
     * 
     * @return 关联的切片实例
     */
    abstract val slice: Slice
}
