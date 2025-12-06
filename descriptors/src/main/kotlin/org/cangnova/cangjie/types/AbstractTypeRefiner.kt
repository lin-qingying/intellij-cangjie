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

import org.cangnova.cangjie.types.model.CangJieTypeMarker


/**
 * 抽象类型精炼器
 *
 * 类型精炼（Type Refinement）是编译器中的一个重要概念，用于在特定上下文中
 * 细化类型信息，使类型更加精确。
 *
 * ## 类型精炼的应用场景
 *
 * 1. **智能类型转换**
 *    ```
 *    if (x is String) {
 *        // 这里 x 的类型被精炼为 String
 *        println(x.length)
 *    }
 *    ```
 *
 * 2. **空值检查**
 *    ```
 *    if (x != null) {
 *        // 这里 x 的类型从 T? 精炼为 T
 *        x.doSomething()
 *    }
 *    ```
 *
 * 3. **多平台项目中的 expect/actual 类型解析**
 *    - 将 expect 声明的类型精炼为平台特定的 actual 实现类型
 *
 * ## 设计模式
 *
 * 这是一个策略模式的应用：
 * - 抽象类定义类型精炼的接口
 * - Default 实现提供默认行为（不做任何精炼）
 * - 具体子类可以实现特定的精炼逻辑
 *
 * @see TypeRefinement 类型精炼 API 标记注解
 * @see CangJieTypeMarker 仓颉类型标记接口
 */
abstract class AbstractTypeRefiner {

    /**
     * 精炼类型
     *
     * 根据特定的上下文信息，将类型转换为更精确的类型。
     *
     * ## 方法职责
     *
     * - 分析输入类型的特征
     * - 根据当前上下文（如控制流分析、类型推断）进行精炼
     * - 返回更精确的类型表示
     *
     * ## 实现要求
     *
     * - **幂等性**: 对同一类型多次调用应产生相同结果
     * - **类型安全**: 精炼后的类型必须是原类型的子类型
     * - **保守性**: 如果无法安全精炼，应返回原类型
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 假设有一个可空类型
     * val nullableType: CangJieTypeMarker = ...
     *
     * // 在某个上下文中，我们知道这个类型不可能为 null
     * val refinedType = refiner.refineType(nullableType)
     * // refinedType 现在是非空类型
     * ```
     *
     * @param type 要精炼的类型
     * @return 精炼后的类型（可能与输入相同）
     *
     * @see TypeRefinement 此方法是类型精炼 API 的一部分
     */
    
    abstract fun refineType(type: CangJieTypeMarker): CangJieTypeMarker

    /**
     * 默认类型精炼器
     *
     * 提供最简单的类型精炼实现：不进行任何精炼，直接返回原类型。
     *
     * ## 使用场景
     *
     * - **占位符**: 在不需要类型精炼的场景中使用
     * - **基准实现**: 作为其他精炼器的对比基准
     * - **测试**: 在测试中作为简单的默认实现
     *
     * ## 设计考虑
     *
     * 使用 object 声明确保单例模式，避免创建多个无状态的实例。
     *
     * @see AbstractTypeRefiner 父类
     */
    object Default : AbstractTypeRefiner() {
        /**
         * 默认精炼实现：直接返回原类型
         *
         * 此实现不执行任何精炼操作，保持类型不变。
         * 这在以下情况下很有用：
         * - 类型系统不支持精炼
         * - 不需要精炼的简单场景
         * - 作为回退机制
         *
         * @param type 输入类型
         * @return 与输入相同的类型
         */
        
        override fun refineType(type: CangJieTypeMarker): CangJieTypeMarker {
            return type
        }
    }
}