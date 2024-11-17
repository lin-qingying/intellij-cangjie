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

package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjVariableDeclaration
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.shouldBeUpdated
import com.linqingying.cangjie.utils.ReadOnly


    abstract class AbstractVariableDescriptor(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    private var _outType: CangJieType?,
    source: SourceElement

) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source),
    VariableDescriptor {
    private var _typeParameters: List<TypeParameterDescriptor> = emptyList()
    protected var _contextReceiverParameters: List<ReceiverParameterDescriptor> = emptyList()
    private var _dispatchReceiverParameter: ReceiverParameterDescriptor? = null
    private var _extensionReceiverParameter: ReceiverParameterDescriptor? = null
    override val original: VariableDescriptor = super.original as VariableDescriptor
    override fun getModality(): Modality {
        return Modality.FINAL
    }
    override val isStatic: Boolean
        get() {

            val element = source.getPsi()
            if(element is CjVariableDeclaration){
                return element.isStatic
            }

            return false
        }
    open fun setOutType(outType: CangJieType) {
        assert(this._outType == null || this._outType.shouldBeUpdated())
        this._outType = outType
    }
    override fun hasSynthesizedParameterNames(): Boolean  = false

    open fun setType(
        outType: CangJieType,
        @ReadOnly typeParameters: List<TypeParameterDescriptor>,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>
    ) {
        setOutType(outType)

        this._typeParameters = typeParameters

        this._extensionReceiverParameter = extensionReceiverParameter
        this._dispatchReceiverParameter = dispatchReceiverParameter
        this._contextReceiverParameters = contextReceiverParameters
    }

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        return _contextReceiverParameters
    }


    override var visibility: DescriptorVisibility = DescriptorVisibilities.LOCAL

    override fun getReturnType(): CangJieType {
        return type
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        return _extensionReceiverParameter
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return _dispatchReceiverParameter
    }

    override val isConst: Boolean
        get() {
            return false
        }


    override fun hasStableParameterNames(): Boolean {
        return false
    }



    companion object {
//        fun create(
//            containingDeclaration: DeclarationDescriptor,
//            annotations: Annotations,
//            modality: Modality,
//            visibility: DescriptorVisibility,
//            isVar: Boolean,
//            name: Name,
//            kind: CallableMemberDescriptor.Kind,
//            source: SourceElement,
//            lateInit: Boolean,
//            isConst: Boolean,
//            isExpect: Boolean,
//            isActual: Boolean,
//            isExternal: Boolean,
//            isDelegated: Boolean
//        ): AbstractVariableDescriptor {
//            return AbstractVariableDescriptor(
//                containingDeclaration, null, annotations,
//                modality, visibility, isVar, name, kind, source, lateInit, isConst,
//                isExpect, isActual, isExternal, isDelegated
//            )
//        }

    }

    override fun getType(): CangJieType {
        return _outType!!
    }


    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return _typeParameters
    }

    //    override fun getOverriddenDescriptors(): Collection<CallableDescriptor> {
//        return emptySet ()
//    }
    override fun getValueParameters(): List<ValueParameterDescriptor> {
        return emptyList()
    }

}
