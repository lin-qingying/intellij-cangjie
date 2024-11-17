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

package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.resolve.descriptorUtil.classValueType
import com.linqingying.cangjie.resolve.descriptorUtil.getClassObjectReferenceTarget
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeSubstitutor
import java.util.*

open class FakeCallableDescriptorForObject(
    val classDescriptor: ClassDescriptor,
) : DeclarationDescriptorWithVisibility by classDescriptor.getClassObjectReferenceTarget(), VariableDescriptor {
    //
//    init {
//        assert(classDescriptor.hasClassValueDescriptor) {
//            "FakeCallableDescriptorForObject can be created only for objects, classes with companion object or enum entries: $classDescriptor"
//        }
//
//    }
    open fun getReferencedDescriptor(): ClassifierDescriptorWithTypeParameters =
        classDescriptor.getClassObjectReferenceTarget()

    fun getReferencedObject(): ClassDescriptor = classDescriptor.getClassObjectReferenceTarget()

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null
    override fun hasSynthesizedParameterNames() = false


    override fun getTypeParameters(): List<TypeParameterDescriptor> = Collections.emptyList()

    override fun hasStableParameterNames() = false

    override fun getValueParameters(): List<ValueParameterDescriptor> = Collections.emptyList()

    override fun getReturnType(): CangJieType? = type

//    override fun hasSynthesizedParameterNames() = false
//
//    override fun hasStableParameterNames() = false

    override fun getOverriddenDescriptors(): Set<CallableDescriptor> = Collections.emptySet()

    override fun getType(): CangJieType = classDescriptor.classValueType!!


    override val original: CallableDescriptor
        get() = this


//

//
    override fun getCompileTimeInitializer() = null

    override fun cleanCompileTimeInitializerCache() {}

    override val source: SourceElement
        get() = classDescriptor.source
    override val isConst: Boolean = false
    override val isVar: Boolean = false

//    override fun isConst(): Boolean = false
//
//    override fun isLateInit(): Boolean = false

    override fun equals(other: Any?) =
        other is FakeCallableDescriptorForObject && classDescriptor == other.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()
    override val containingDeclaration: DeclarationDescriptor
        get() = classDescriptor.getClassObjectReferenceTarget().containingDeclaration

    override fun getModality(): Modality {
        return Modality.FINAL
    }


    override fun substitute(substitutor: TypeSubstitutor) = this

//    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
