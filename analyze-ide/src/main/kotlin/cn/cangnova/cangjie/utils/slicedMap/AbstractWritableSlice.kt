/*
 * Copyright 2024 LinQingYing. and contributors.
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

/**
 * 可写切片的抽象实现类
 * 
 * 这个类同时实现了KeyWithSlice和WritableSlice接口，为可写切片提供了基础实现。
 * 它简化了可写切片的创建过程，通过将自身同时作为键和切片使用。
 *
 * @param K 键的类型
 * @param V 值的类型
 * @param debugName 用于调试的名称
 */
abstract class AbstractWritableSlice<K, V>(debugName: String) : KeyWithSlice<K, V, WritableSlice<K, V>>(debugName),
    WritableSlice<K, V> {
    /**
     * 获取与此键关联的切片
     * 
     * 在此实现中，切片就是当前对象自身
     * 
     * @return 当前对象作为切片
     */
    override val slice: WritableSlice<K, V>
        get() = this

    /**
     * 获取与此切片关联的键
     * 
     * 在此实现中，键就是当前对象自身
     * 
     * @return 当前对象作为键
     */
    override val key: KeyWithSlice<K, V, WritableSlice<K, V>>
        get() = this
}
