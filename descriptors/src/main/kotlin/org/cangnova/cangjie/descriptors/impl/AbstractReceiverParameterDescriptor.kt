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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor
import org.cangnova.cangjie.types.Variance

/**
 * 抽象接收者参数描述符的基类实现。
 *
 * 该类实现了 ReceiverParameterDescriptor 的大部分通用逻辑，作为具体接收者参数描述符（如方法的 this 接收者）的基类。
 * 它封装了与接收者相关的类型、注解、所属声明以及类型替换（substitute）逻辑。
 * 子类可根据需要扩展或覆盖部分行为。
 *
 * @param annotations 接收者的注解集合
 * @param name 接收者的名称，默认为 SpecialNames.THIS
 */
abstract class AbstractReceiverParameterDescriptor(annotations: Annotations, name: Name = SpecialNames.THIS) :
    DeclarationDescriptorImpl(annotations, name), ReceiverParameterDescriptor {

    /**
     * 返回原始参数描述符（用于替换/回溯），此处指向自身。
     */
    override val original: ParameterDescriptor
        get() = this

    /**
     * 接受访问者模式的实现，用于把当前接收者描述符派发给访问者处理。
     */
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

    override val typeParameters: List<TypeParameterDescriptor>
        get() = emptyList()

    override val type: CangJieType
        get() = value.type


    override val source: SourceElement
        get() = SourceElement.NO_SOURCE

    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.LOCAL


    /**
     * 使用给定的类型替换器对接收者类型进行替换。
     * 若替换器为空则直接返回当前实例；若替换后类型与原类型相同则返回当前实例；
     * 若替换失败或结果为 null 则返回 null；否则返回新的 ReceiverParameterDescriptorImpl 实例。
     *
     * @param substitutor 类型替换器
     * @return 替换后的 ReceiverParameterDescriptor 或 null
     */
    override fun substitute(substitutor: TypeSubstitutor): ReceiverParameterDescriptor? {
        if (substitutor.isEmpty) return this

        val substitutedType: CangJieType?

        substitutedType = substitutor.substitute(type, Variance.INVARIANT)


        if (substitutedType == null) return null
        if (substitutedType === type) return this

        return ReceiverParameterDescriptorImpl(containingDeclaration, TransientReceiver(substitutedType), annotations)
    }
}
