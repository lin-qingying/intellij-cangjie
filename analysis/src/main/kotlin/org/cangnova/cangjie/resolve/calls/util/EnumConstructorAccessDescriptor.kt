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

package org.cangnova.cangjie.resolve.calls.util

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor
import java.util.*

/**
 * 枚举构造器的假可调用描述符
 *
 * 用于支持仓颉语言中枚举构造器的值访问（如 Color.Red）。
 * 这是一个包装类，将枚举类型本身包装为可调用的变量描述符，
 * 使得枚举构造器可以像变量一样被引用和访问。
 *
 * @param classDescriptor 枚举类描述符
 */
open class EnumConstructorAccessDescriptor(
    val classDescriptor: ClassDescriptor,
) : DeclarationDescriptorWithVisibility by classDescriptor, VariableDescriptor {

    open fun getReferencedDescriptor(): ClassifierDescriptorWithTypeParameters = classDescriptor

    fun getReferencedObject(): ClassDescriptor = classDescriptor

    override val contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList()

    override val extensionReceiverParameter: ReceiverParameterDescriptor? = null

    override val dispatchReceiverParameter: ReceiverParameterDescriptor? = null

    override fun hasSynthesizedParameterNames() = false

    override val typeParameters: List<TypeParameterDescriptor> = Collections.emptyList()

    override fun hasStableParameterNames() = false

    override val valueParameters: List<ValueParameterDescriptor> = Collections.emptyList()

    override val returnType: CangJieType? = type

    override val overriddenDescriptors: Collection<CallableDescriptor> = Collections.emptySet()

    override val type: CangJieType
        get() = classDescriptor.defaultType  // 枚举类的默认类型

    override val original: CallableDescriptor
        get() = this

    override fun getCompileTimeInitializer() = null

    override fun cleanCompileTimeInitializerCache() {}

    override val source: SourceElement
        get() = classDescriptor.source

    override val isConst: Boolean = false
    override val isVar: Boolean = false

    override fun equals(other: Any?) =
        other is EnumConstructorAccessDescriptor && classDescriptor == other.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()

    override val containingDeclaration: DeclarationDescriptor
        get() = classDescriptor.containingDeclaration

    override val modality: Modality = Modality.FINAL

    override fun substitute(substitutor: TypeSubstitutor) = this
}
