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

package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.types.TypeSubstitutor

/**
 * Substitutable接口定义了能够进行类型替换的声明描述符的通用行为
 * 它允许在给定类型替换器的情况下，替换类型参数或类型引用
 *
 * @param <out T> 泛型参数，表示实现此接口的声明描述符类型，限定为DeclarationDescriptorNonRoot的子类型
 *                使用out关键字表示此泛型参数是协变的，即可以作为函数返回值类型，但不能作为参数类型
 */
interface Substitutable<out T : DeclarationDescriptorNonRoot> {
    /**
     * 使用给定的类型替换器对此声明描述符进行类型替换
     *
     * @param substitutor 类型替换器，用于执行类型替换操作
     * @return T 返回替换后的声明描述符，类型与接口泛型参数T相同
     */
    fun substitute(substitutor: TypeSubstitutor): T
}
