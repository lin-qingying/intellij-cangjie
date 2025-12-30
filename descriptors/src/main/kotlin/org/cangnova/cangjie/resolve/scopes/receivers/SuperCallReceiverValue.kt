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

package org.cangnova.cangjie.resolve.scopes.receivers

import org.cangnova.cangjie.types.CangJieType

/**
 * Super 调用的接收器值
 *
 * SuperCallReceiverValue 表示在调用父类方法时使用的接收器。
 * 它同时包含两个类型信息：
 *
 * 1. [type]（继承自 ReceiverValue）：用于调用的具体父类型（super-type）
 *    这是父类类型中的某一个特定类型，用于确定调用哪个父类的方法。
 *
 * 2. [thisType]：实际的接收器类型，即当前对象（this）的类型
 *    这是调用时实际使用的类型，包含了所有泛型参数的实例化信息。
 *
 * 例如在以下代码中：
 * ```
 * class Child : Parent<Int> {
 *     func foo() {
 *         super.bar()  // 这里的接收器
 *     }
 * }
 * ```
 * - [type] 是 `Parent<Int>`（用于查找 bar 方法的具体父类型）
 * - [thisType] 是 `Child`（实际的接收器对象类型）
 *
 * @property thisType 实际的接收器类型，用于调用父类描述符
 *
 * @see ReceiverValue
 */
interface SuperCallReceiverValue : ReceiverValue {
    /**
     * 实际的接收器类型
     *
     * 这是调用父类方法时实际使用的对象类型，而 [ReceiverValue.type] 是具体的父类型。
     */
    val thisType: CangJieType
}
