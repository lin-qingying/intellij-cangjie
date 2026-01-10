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

package org.cangnova.cangjie.resolve.scopes.receivers

import org.cangnova.cangjie.types.CangJieType

/**
 * 瞬态接收器
 *
 * TransientReceiver 表示在 for 循环中 hasNext 和 next() 方法调用时的接收器。
 * 这是一个特殊的接收器类型，因为它不能是表达式接收器 —— iterator() 调用没有对应的表达式。
 *
 * 在仓颉语言的 for 循环中：
 * ```
 * for (item in collection) { ... }
 * ```
 * 实际上会被转换为：
 * ```
 * val iterator = collection.iterator()
 * while (iterator.hasNext()) {
 *     val item = iterator.next()
 *     ...
 * }
 * ```
 *
 * 这里的 `iterator` 变量就是一个 TransientReceiver，它是临时生成的，
 * 不对应源代码中的任何显式表达式。
 *
 * @param type 接收器的类型（通常是 Iterator 类型）
 * @param original 原始的接收器值，用于类型替换链
 *
 * @see AbstractReceiverValue
 */
class TransientReceiver private constructor(
    type:  CangJieType,
    original:  ReceiverValue?
) :
     AbstractReceiverValue(type, original) {
    /**
     * 公共构造函数
     *
     * @param type 接收器的类型
     */
    constructor(type:  CangJieType) : this(type, null)

    override fun toString(): String {
        return "{Transient} : " + type
    }

    /**
     * 创建一个具有新类型的瞬态接收器副本
     *
     * @param newType 新的类型
     * @return 具有新类型的瞬态接收器
     */
    override fun replaceType(newType:  CangJieType):  ReceiverValue {
        return TransientReceiver(newType, original)
    }
}
