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
 * 数据包装盒
 *
 * 该类用于包装任意类型的数据，并屏蔽数据本身的 [hashCode] 和 [equals] 方法，
 * 使用引用相等性代替值相等性。
 *
 * 使用场景：
 * - 需要在 Set 或 Map 中使用引用相等性而非值相等性
 * - 避免触发数据对象的 hashCode/equals 计算（可能很昂贵）
 * - 需要区分相同值但不同实例的对象
 *
 * 实现特点：
 * - 使用引用相等性（identity equality）
 * - 不调用被包装数据的 hashCode 和 equals 方法
 * - 轻量级包装，没有额外开销
 *
 * 示例：
 * ```kotlin
 * val data1 = "hello"
 * val data2 = "hello"
 * val box1 = Box(data1)
 * val box2 = Box(data2)
 *
 * println(data1 == data2)      // true (值相等)
 * println(box1 == box2)        // false (引用不同)
 * println(box1 === box1)       // true (同一个引用)
 * ```
 *
 * @param T 被包装的数据类型
 * @property data 被包装的数据
 */
class Box<T>(val data: T) {
    /**
     * 返回对象的哈希码
     *
     * 使用父类的 hashCode 实现（基于对象引用），
     * 而不调用被包装数据的 hashCode 方法。
     *
     * @return 对象的哈希码
     */
    override fun hashCode(): Int {
        return super.hashCode() // This class is needed to screen from calling data's hashCode()
    }

    /**
     * 检查相等性
     *
     * 使用父类的 equals 实现（基于引用相等性），
     * 而不调用被包装数据的 equals 方法。
     *
     * @param other 要比较的对象
     * @return 如果是同一个对象返回 true，否则返回 false
     */
    override fun equals(other: Any?): Boolean {
        return super.equals(other) // This class is needed to screen from calling data's equals()
    }
}
