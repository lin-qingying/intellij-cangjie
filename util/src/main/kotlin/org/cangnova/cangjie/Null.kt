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

package org.cangnova.cangjie

/**
 * 空值单例对象
 *
 * 该对象提供了一个类型安全的空值表示，用于区分实际的 null 值。
 *
 * 使用场景：
 * - 需要区分"未设置"和"设置为 null"的情况
 * - 作为 Map 中的特殊标记值
 * - 在不支持 null 的上下文中表示空值
 *
 * 实现特点：
 * - 单例模式，全局唯一
 * - hashCode 始终返回 0
 * - 只与自身相等（使用引用相等性）
 *
 * 示例：
 * ```kotlin
 * val map = mutableMapOf<String, Any>()
 * map["key"] = Null  // 标记为空值，而不是 null
 *
 * if (map["key"] === Null) {
 *     println("Value is marked as Null")
 * }
 * ```
 */
object Null : Any() {
    /**
     * 返回固定的哈希码 0
     *
     * @return 始终返回 0
     */
    override fun hashCode(): Int {
        return 0
    }

    /**
     * 返回字符串表示
     *
     * @return 字符串 "Null"
     */
    override fun toString(): String = "Null"

    /**
     * 检查相等性
     *
     * 只有当 other 与当前对象是同一个实例时才返回 true。
     *
     * @param other 要比较的对象
     * @return 如果是同一个实例返回 true，否则返回 false
     */
    override fun equals(other: Any?): Boolean = other === this
}