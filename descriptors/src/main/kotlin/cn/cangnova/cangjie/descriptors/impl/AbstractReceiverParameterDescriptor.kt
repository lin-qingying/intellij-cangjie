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
package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.name.SpecialNames
import cn.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeSubstitutor
import cn.cangnova.cangjie.types.Variance

abstract class AbstractReceiverParameterDescriptor(annotations: Annotations, name: Name = SpecialNames.THIS) :
    DeclarationDescriptorImpl(annotations, name), ReceiverParameterDescriptor {

    override val original: ParameterDescriptor
        get() = this


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitReceiverParameterDescriptor(this, data)

    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override val dispatchReceiverParameter: ReceiverParameterDescriptor?
        get() = null

    override val valueParameters: List<ValueParameterDescriptor>
        get() = emptyList()

    override val contextReceiverParameters: List<ReceiverParameterDescriptor>
        get() = emptyList()

    override val returnType: CangJieType?
        get() = type

    override val extensionReceiverParameter: ReceiverParameterDescriptor?
        get() = null

    override val overriddenDescriptors: Collection<CallableDescriptor>
        get() = emptySet()

    override val typeParameters: Collection<TypeParameterDescriptor>
        get() = emptyList()

    override val type: CangJieType
        get() = value.type


    override val source: SourceElement
        get() = SourceElement.NO_SOURCE

    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.LOCAL


    override fun substitute(substitutor: TypeSubstitutor): ReceiverParameterDescriptor? {
        if (substitutor.isEmpty) return this

        val substitutedType: CangJieType?

        substitutedType = substitutor.substitute(type, Variance.INVARIANT)


        if (substitutedType == null) return null
        if (substitutedType === type) return this

        return ReceiverParameterDescriptorImpl(containingDeclaration, TransientReceiver(substitutedType), annotations)
    }
}
