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
package org.cangnova.cangjie.types

import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.TypeArgumentMarker

/**
 * 类型投影接口
 *
 * 表示泛型类型参数的投影，包含类型和变异性（variance）信息。
 * 类型投影用于表示泛型参数的协变、逆变或不变关系。
 *
 * 例如在 `List<out T>` 中，`out T` 就是一个类型投影，
 * 其中 `out` 表示协变（covariance），`T` 是类型。
 *
 * 使用场景：
 * - 泛型类型参数的表示
 * - 类型系统中的子类型关系判定
 * - 类型推导和类型检查
 *
 * @see Variance 变异性枚举
 * @see CangJieType 仓颉类型接口
 */
interface TypeProjection : TypeArgumentMarker {

    /**
     * 投影的变异性
     *
     * 表示类型参数的变异性：
     * - [Variance.INVARIANT]: 不变（既不协变也不逆变）
     * - [Variance.IN_VARIANCE]: 逆变（contravariance），对应 `in` 关键字
     * - [Variance.OUT_VARIANCE]: 协变（covariance），对应 `out` 关键字
     */
    val projectionKind: Variance

    /**
     * 投影的类型
     *
     * 表示实际的类型参数，例如在 `List<out String>` 中，此属性返回 `String` 类型。
     */
    val type: CangJieType

    //    boolean isStarProjection();

    /**
     * 精化类型投影
     *
     * 使用类型精化器对当前类型投影进行精化处理。
     * 类型精化用于在类型检查和推导过程中细化类型信息。
     *
     * @param cangjieTypeRefiner 仓颉类型精化器
     * @return 精化后的类型投影
     */
    fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeProjection

    /**
     * 替换投影中的类型
     *
     * 创建一个新的类型投影，保持相同的变异性，但使用指定的新类型。
     *
     * @param type 新的类型
     * @return 使用新类型的类型投影
     */
    fun replaceType(type: CangJieType): TypeProjection
}
