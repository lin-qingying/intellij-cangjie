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

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.resolve.constants.ConstantValue
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeSubstitutor

open class ValueParameterDescriptorImpl(
    containingDeclaration: CallableDescriptor,
    original: ValueParameterDescriptor?,
    override val index: Int,
    annotations: Annotations,
    name: Name,
    override val isNamed: Boolean,
    outType: CangJieType,
    private val declaresDefaultValue: Boolean,

    source: SourceElement,

    ) : AbstractVariableDescriptor(containingDeclaration, annotations, name, outType, source),
    ValueParameterDescriptor {

    companion object {
        @JvmStatic
        fun createWithDestructuringDeclarations(
            containingDeclaration: CallableDescriptor,
            original: ValueParameterDescriptor?,
            index: Int,
            annotations: Annotations,
            name: Name,
            isNamed: Boolean,
            outType: CangJieType,
            declaresDefaultValue: Boolean,

            source: SourceElement,
            destructuringVariables: (() -> List<VariableDescriptor>)? = null
        ): ValueParameterDescriptorImpl = if (destructuringVariables == null) ValueParameterDescriptorImpl(
            containingDeclaration,
            original,
            index,
            annotations,
            name,
            isNamed,
            outType,
            declaresDefaultValue,
            source
        )
        else WithDestructuringDeclaration(
            containingDeclaration,
            original,
            index,
            annotations,
            name,
            isNamed,
            outType,
            declaresDefaultValue,
            source,
            destructuringVariables
        )
    }

    class WithDestructuringDeclaration internal constructor(
        containingDeclaration: CallableDescriptor, original: ValueParameterDescriptor?, index: Int,

        annotations: Annotations, name: Name, isNamed: Boolean, outType: CangJieType, declaresDefaultValue: Boolean,

        source: SourceElement, destructuringVariables: () -> List<VariableDescriptor>
    ) : ValueParameterDescriptorImpl(
        containingDeclaration, original, index, annotations, name, isNamed, outType, declaresDefaultValue,

        source
    ) {

        val destructuringVariables by lazy(destructuringVariables)

        override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
            return WithDestructuringDeclaration(
                newOwner,
                null,
                newIndex,
                annotations,
                newName,
                isNamed,
                type,
                declaresDefaultValue(),
                SourceElement.NO_SOURCE
            ) { destructuringVariables }
        }
    }

    override val varargElementType: CangJieType?
        get() {
//             这里应该是当形参最后一个类型是Array时，并且不为命名参数，才是vararg
            if (isNamed) return null

//            判断是不是最后一个参数
            if (containingDeclaration is FunctionDescriptor && containingDeclaration.valueParameters.last() != this) {
                return null
            }
            if (!CangJieBuiltIns.isArray(this.type)) {
                return null
            }

            return type.arguments[0].type

        }
    private val myOriginal: ValueParameterDescriptor = original ?: this

    override val original
        get() = if (myOriginal === this) this else myOriginal.original

    override val isVar: Boolean
        get() = false

    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
        return ValueParameterDescriptorImpl(
            newOwner,
            null,
            newIndex,
            annotations,
            newName,
            isNamed,
            type,
            declaresDefaultValue(),
            SourceElement.NO_SOURCE
        )
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun cleanCompileTimeInitializerCache() {

    }

    override fun declaresDefaultValue(): Boolean {
        return declaresDefaultValue && (containingDeclaration as CallableMemberDescriptor).kind.isReal
    }

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
        if (substitutor.isEmpty) return this
        throw UnsupportedOperationException() // TODO
    }


    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> {
        return containingDeclaration.overriddenDescriptors.map {
            it.valueParameters[index]
        }
    }


    override val containingDeclaration
        get() = super.containingDeclaration as CallableDescriptor

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {

        return visitor.visitValueParameterDescriptor(this, data!!)

    }


}
