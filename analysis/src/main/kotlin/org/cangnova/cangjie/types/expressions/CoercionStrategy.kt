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

package org.cangnova.cangjie.types.expressions

/**
 * 类型强制转换策略
 *
 * 定义表达式类型检查过程中的强制转换行为。
 *
 * ## 策略类型
 *
 * ### NO_COERCION
 * 不进行任何类型强制转换，保持表达式的原始类型。
 *
 * **使用场景**:
 * - 表达式类型已匹配预期类型
 * - 需要保留精确的类型信息
 * - 子表达式的类型检查
 *
 * **示例**:
 * ```kotlin
 * val x: Int = 42        // NO_COERCION: Int 直接匹配
 * val y: String = "abc"  // NO_COERCION: String 直接匹配
 * ```
 *
 * ### COERCION_TO_UNIT
 * 将表达式的类型强制转换为 Unit，丢弃表达式的返回值。
 *
 * **使用场景**:
 * - 语句上下文中使用表达式（期望类型为 Unit）
 * - 忽略函数返回值
 * - 在 Unit 上下文中执行副作用操作
 *
 * **示例**:
 * ```kotlin
 * fun foo(): Int = 42
 *
 * fun bar() {
 *     foo()  // COERCION_TO_UNIT: Int 被强制转换为 Unit
 *     // 等价于：foo(); Unit
 * }
 * ```
 *
 * ## 类型强制转换的语义
 *
 * 强制转换并不改变表达式的实际执行，只是调整类型系统的处理方式：
 * - 表达式仍然会被求值
 * - 副作用会正常发生
 * - 仅影响类型检查器对返回值的处理
 *
 * ## 在表达式类型检查中的使用
 *
 * ```kotlin
 * // ExpressionTypingVisitor 中的使用示例
 * val coercionStrategy = when {
 *     context.expectedType.isUnit() -> CoercionStrategy.COERCION_TO_UNIT
 *     else -> CoercionStrategy.NO_COERCION
 * }
 * ```
 */
enum class CoercionStrategy {
    /**
     * 不进行类型强制转换
     *
     * 保持表达式的原始类型不变
     */
    NO_COERCION,

    /**
     * 强制转换为 Unit 类型
     *
     * 将表达式的返回值类型转换为 Unit，用于语句上下文
     */
    COERCION_TO_UNIT
}
