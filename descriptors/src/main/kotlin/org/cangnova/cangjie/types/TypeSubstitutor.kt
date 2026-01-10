/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.types.model.TypeSubstitutorMarker

/**
 * 新的类型替换器接口
 *
 * 这是一个简化的类型替换器接口，用于类型推导系统。
 * 相比传统的 DefaultTypeSubstitutor，它提供了更简洁的 API。
 *
 * @see DefaultTypeSubstitutor 传统的类型替换器实现
 */
interface  TypeSubstitutor : TypeSubstitutorMarker {
    /**
     * 安全地替换类型，如果替换失败则返回原类型
     */
    fun safeSubstitute(type: UnwrappedType): UnwrappedType

    /**
     * 根据类型构造器替换非空类型
     *
     * @param constructor 类型构造器
     * @return 替换后的类型，如果无法替换则返回 null
     */
    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?

    /**
     * 判断替换器是否为空（没有任何替换规则）
     */
    val isEmpty: Boolean
}