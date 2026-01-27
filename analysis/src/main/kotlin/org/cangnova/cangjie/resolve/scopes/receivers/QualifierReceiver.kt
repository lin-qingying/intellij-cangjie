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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType

// 详细接收器标记接口，仅用于解析过程中的类型区分
/**
 * 详细接收器标记接口
 *
 * DetailedReceiver 是一个标记接口，用于标识那些在解析过程中需要额外信息的接收器。
 * 这个接口本身不定义任何成员，仅用于类型区分。
 *
 * @see QualifierReceiver
 * @see ReceiverValueWithSmartCastInfo
 */
interface DetailedReceiver

/**
 * 限定符接收器
 *
 * QualifierReceiver 表示用作限定符的接收器，如包名、类名等。
 * 这种接收器仅用于解析过程，不是真正的运行时值。
 *
 * 在仓颉语言中，类不能作为值使用，只能作为类型限定符。
 * 因此 QualifierReceiver 不提供类值接收器(classValueReceiver)。
 *
 * 例如在以下代码中：
 * ```
 * package.ClassName.staticMethod()
 * ```
 * `package` 和 `ClassName` 都是 QualifierReceiver。
 *
 * @property descriptor 限定符对应的声明描述符
 * @property staticScope 限定符的静态作用域，包含可以通过此限定符访问的成员
 *
 * @see Qualifier
 * @see DetailedReceiver
 */
interface QualifierReceiver : Receiver, DetailedReceiver {
    /** 限定符对应的声明描述符 */
    val descriptor: DeclarationDescriptor

    /** 限定符的静态作用域 */
    val staticScope: MemberScope
}


/**
 * 表示带有智能转换信息的接收者值
 *
 * 该类用于跟踪接收者值及其智能转换类型信息。
 * 智能转换是指编译器根据控制流分析推断出的更精确的类型。
 *
 * 例如在以下代码中：
 * ```
 * val obj: Any = "hello"
 * if (obj is String) {
 *     // 这里 obj 被智能转换为 String 类型
 *     obj.length  // 可以访问 String 的成员
 * }
 * ```
 *
 * @property receiverValue 接收者值
 * @property typesFromSmartCasts 智能转换得到的类型集合，不包括 receiverValue.type。
 *                                这些类型仅用于特殊标记（例如，IDE 中的绿色高亮显示），
 *                                但不用于构造最终类型。
 * @property isStable 表示接收者值是否稳定。稳定的接收者（如局部变量）可以进行智能转换，
 *                    不稳定的接收者（如可变属性）不能智能转换。
 * @property allOriginalTypes 用于构造最终类型的所有类型，包括原始基础类型和智能转换类型
 *
 * @see QualifierReceiver
 * @see DetailedReceiver
 */
class ReceiverValueWithSmartCastInfo(
    val receiverValue: ReceiverValue,
    val typesFromSmartCasts: Set<CangJieType>,
    val isStable: Boolean,
    originalBaseType: CangJieType = receiverValue.type
) : DetailedReceiver {
    /** 用于构造最终类型的所有原始类型（基础类型 + 智能转换类型） */
    val allOriginalTypes = typesFromSmartCasts + originalBaseType

    /**
     * 检查是否存在智能转换类型
     *
     * @return 如果存在智能转换类型则返回 true，否则返回 false
     */
    fun hasTypesFromSmartCasts() = typesFromSmartCasts.isNotEmpty()

    override fun toString() = receiverValue.toString()
}
