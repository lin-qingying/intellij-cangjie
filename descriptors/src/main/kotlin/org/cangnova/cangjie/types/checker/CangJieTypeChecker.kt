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
package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor

/**
 * CangJie 类型检查器接口。
 *
 * 提供类型相等性与子类型判断的策略点。实现可以基于不同规则（例如泛型比较策略、原始类型比较等）
 * 来定制类型比较行为。
 */
interface CangJieTypeChecker {
    /**
     * 类型构造器相等性的策略函数接口。
     *
     * 当比较两个类型时，有时需要自定义如何判断它们的 TypeConstructor 是否相等。
     */
    fun interface TypeConstructorEquality {
        /**
         * 判断两个 TypeConstructor 是否相等。
         *
         * @return true 表示相等，false 表示不相等
         */
        fun equals(a: TypeConstructor, b: TypeConstructor): Boolean
    }

    /**
     * 在比较两个类型时忽略泛型参数，仅比较原始/裸类型是否相同。
     *
     * 例如 List<String> 与 List<Int> 在此比较下可能被视为相等。
     *
     * @return true 表示在忽略泛型后两类型相等
     */
    fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean

    /**
     * 判断 subtype 是否为 supertype 的子类型。
     *
     * @return true 表示 subtype 被视为 supertype 的子类型（通过检查或推断），false 表示不是
     */
    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean

    /**
     * 判断两个类型是否完全相等（考虑泛型等所有细节）。
     *
     * @return true 表示完全相等，false 表示不相等
     */
    fun equalTypes(a: CangJieType, b: CangJieType): Boolean

    companion object {
        /**
         * 默认的类型检查器实现引用。
         */
        val DEFAULT: CangJieTypeChecker =  NewCangJieTypeChecker.Default
    }
}
