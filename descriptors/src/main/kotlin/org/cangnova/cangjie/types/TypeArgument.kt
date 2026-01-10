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

import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.TypeArgumentMarker
/**
 * 类型实参
 *
 * 表示泛型类型的实际类型参数。当泛型类型被实例化时，需要为每个类型参数提供具体的类型实参。
 *
 * ## 概念说明
 * - **类型参数（Type Parameter）**：泛型声明时的占位符，如 `class List<T>` 中的 `T`
 * - **类型实参（Type Argument）**：泛型使用时的具体类型，如 `List<String>` 中的 `String`
 *
 * ## 使用示例
 * ```
 * List<String>           // String 是类型实参
 * Map<String, Int>       // String 和 Int 是两个类型实参
 * List<List<String>>     // 外层的 List<String> 是类型实参
 * ```
 *
 * ## 核心功能
 * 此接口提供了类型实参的三个核心能力：
 * 1. 持有具体的类型信息
 * 2. 支持类型精化，用于类型检查和推导
 * 3. 支持类型替换，用于泛型实例化
 */
interface TypeArgument : TypeArgumentMarker {

    /**
     * 类型实参
     *
     * 表示泛型类型的实际类型参数。
     * 例如在 `List<String>` 中，此属性返回 `String` 类型。
     * 在 `Map<String, Int>` 中，有两个类型实参分别为 `String` 和 `Int`。
     */
    val type: CangJieType

    /**
     * 精化类型实参
     *
     * 使用类型精化器对当前类型实参进行精化处理。
     * 类型精化用于在类型检查和类型推导过程中细化类型信息，
     * 例如在处理泛型约束、子类型关系判断时。
     *
     * @param cangjieTypeRefiner 仓颉类型精化器
     * @return 精化后的类型实参
     */
    fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeArgument

    /**
     * 替换类型实参
     *
     * 创建一个新的类型实参对象，使用指定的新类型替换当前类型。
     * 此方法用于泛型类型的实例化过程，例如将泛型类型 `List<T>`
     * 实例化为具体类型 `List<String>`。
     *
     * @param type 新的类型
     * @return 包含新类型的类型实参
     */
    fun replaceType(type: CangJieType): TypeArgument
}