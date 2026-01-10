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
 * 具有类型信息的接收器值
 *
 * ReceiverValue 表示一个带有具体类型的接收器，用于类型检查和推导。
 * 它扩展了基础的 [Receiver] 接口，添加了类型信息和类型替换的能力。
 *
 * 在类型推导过程中，接收器的类型可能会被替换（如泛型实例化），
 * [replaceType] 方法用于创建具有新类型的接收器值的副本。
 *
 * [original] 属性用于在类型替换链中追溯到最初的接收器值，
 * 这对于保持类型推导的正确性和避免循环引用非常重要。
 *
 * @property type 接收器的类型
 * @property original 原始的接收器值（在类型替换前的值）
 *
 * @see Receiver
 * @see CangJieType
 */
interface ReceiverValue : Receiver {
    /** 接收器的类型 */
    val type: CangJieType

    /**
     * 创建一个具有新类型的接收器值副本
     *
     * @param newType 新的类型
     * @return 具有新类型的接收器值
     */
    fun replaceType(newType: CangJieType): ReceiverValue

    /** 原始的接收器值（在任何类型替换之前） */
    val original: ReceiverValue
}
