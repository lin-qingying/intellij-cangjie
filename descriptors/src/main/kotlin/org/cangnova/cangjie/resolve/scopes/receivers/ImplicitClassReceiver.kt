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

import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.types.CangJieType

/**
 * 类内部的任何 "this" 接收器
 *
 * ThisClassReceiver 表示在类的作用域内可以访问的 this 接收器。
 * 它不仅包含类型信息，还保留了对类描述符的引用。
 *
 * @property classDescriptor 类的描述符
 *
 * @see ImplicitClassReceiver
 */
interface ThisClassReceiver : ReceiverValue {
    /** 类的描述符 */
    val classDescriptor: ClassAndEnumDescriptor
}

/**
 * 类内部的隐式 "this" 接收器
 *
 * ImplicitClassReceiver 表示在类的成员函数或属性中隐式可用的 this 接收器。
 * 例如，在类的成员函数中，可以直接访问其他成员而不需要显式写出 "this."。
 *
 * 这个类是不可变的，但支持通过构造函数传入 original 参数来创建类型替换后的副本。
 * 不支持通过 [replaceType] 方法替换类型，因为类的类型应该始终是类的默认类型。
 *
 * @param classDescriptor 类的描述符
 * @param original 原始的接收器（用于类型替换链），如果为 null，则当前实例为原始值
 *
 * @property type 接收器的类型，总是返回类的默认类型
 * @property declarationDescriptor 声明此接收器的描述符，即类描述符
 *
 * @see ThisClassReceiver
 * @see ImplicitReceiver
 */
open class ImplicitClassReceiver(
    final override val classDescriptor: ClassAndEnumDescriptor,
    original: ImplicitClassReceiver? = null
) : ThisClassReceiver, ImplicitReceiver {

    /** 原始的接收器值 */
    override val original = original ?: this

    /** 接收器的类型，总是返回类的默认类型 */
    override val type: CangJieType
        get() = classDescriptor.defaultType

    /** 声明此接收器的描述符，即类描述符 */
    override val declarationDescriptor = classDescriptor

    override fun equals(other: Any?) = classDescriptor == (other as? ImplicitClassReceiver)?.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()

    override fun toString() = "Class{$type}"

    /**
     * 不支持替换类型
     *
     * 类接收器的类型应该始终是类的默认类型，不应该被替换。
     *
     * @throws UnsupportedOperationException 总是抛出此异常
     */
    override fun replaceType(newType: CangJieType) =
        throw UnsupportedOperationException("Replace type should not be called for this receiver")


}
