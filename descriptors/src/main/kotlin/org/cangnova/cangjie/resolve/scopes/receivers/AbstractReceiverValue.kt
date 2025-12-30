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
 * ReceiverValue 的抽象基类实现
 *
 * 这个抽象类提供了 [ReceiverValue] 接口的基本实现，特别是处理了 [original] 属性的逻辑。
 * 它确保了类型替换链的正确性：
 * - 如果传入的 original 参数不为 null，则使用该参数作为原始值
 * - 如果传入的 original 参数为 null，则将当前实例作为原始值（表示这是类型替换链的起点）
 *
 * 具体的接收器类型应该继承这个抽象类，并实现 [ReceiverValue.replaceType] 方法。
 *
 * @param type 接收器的类型
 * @param original 原始的接收器值，如果为 null，则当前实例为原始值
 *
 * @see ReceiverValue
 */
abstract class AbstractReceiverValue(override val type: CangJieType, original: ReceiverValue?) : ReceiverValue {

    /** 原始的接收器值，如果构造时未提供，则为当前实例本身 */
    override val original: ReceiverValue = if (original != null) original else this

}
