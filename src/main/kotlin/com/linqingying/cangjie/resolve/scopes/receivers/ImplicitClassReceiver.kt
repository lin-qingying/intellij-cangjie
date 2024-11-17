/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.types.CangJieType
import java.lang.UnsupportedOperationException

/**
 * Describes any "this" receiver inside a class
 */
interface ThisClassReceiver : ReceiverValue {
    val classDescriptor: ClassDescriptor
}
/**
 * Same but implicit only
 */
open class ImplicitClassReceiver(
    final override val classDescriptor: ClassDescriptor,
    original: ImplicitClassReceiver? = null
) : ThisClassReceiver, ImplicitReceiver {

    private val original = original ?: this

    override fun getType() = classDescriptor.defaultType

    override val declarationDescriptor = classDescriptor

    override fun equals(other: Any?) = classDescriptor == (other as? ImplicitClassReceiver)?.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()

    override fun toString() = "Class{$type}"

    override fun replaceType(newType: CangJieType) =
        throw UnsupportedOperationException("Replace type should not be called for this receiver")

    override fun getOriginal() = original
}
