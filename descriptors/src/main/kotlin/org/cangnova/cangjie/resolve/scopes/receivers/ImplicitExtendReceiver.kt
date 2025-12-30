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

import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.types.CangJieType

/**
 * 表示扩展（extend）内的隐式接收器
 *
 * 用于扩展块内成员函数的 this 接收器，类型为被扩展的类型（extendType）
 */
open class ImplicitExtendReceiver(
    val extendDescriptor: ExtendDescriptor,
    original: ImplicitExtendReceiver? = null
) : ImplicitReceiver {

    override val original = original ?: this

    override val type: CangJieType
        get() = extendDescriptor.extendType

    override val declarationDescriptor = extendDescriptor

    override fun equals(other: Any?) = extendDescriptor == (other as? ImplicitExtendReceiver)?.extendDescriptor

    override fun hashCode() = extendDescriptor.hashCode()

    override fun toString() = "Extend{$type}"

    override fun replaceType(newType: CangJieType) =
        throw UnsupportedOperationException("Replace type should not be called for extend receiver")
}
