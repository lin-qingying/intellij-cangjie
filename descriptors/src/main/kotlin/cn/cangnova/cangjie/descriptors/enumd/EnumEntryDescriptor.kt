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

package cn.cangnova.cangjie.descriptors.enumd

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.resolve.lazy.LazyClassContext
import cn.cangnova.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyClassMemberScope
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeSubstitutor

class EnumEntryCallableMemberDescriptor(
    val entry: EnumEntryDescriptor
) : CallableMemberDescriptor {
    override fun getValueParameters(): List<ValueParameterDescriptor> = emptyList()

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitClassDescriptor(entry, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        TODO()
    }

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
        return this
    }

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getReturnType(): CangJieType? = null

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getOverriddenDescriptors(): List<CallableMemberDescriptor> = emptyList()

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return emptyList()
    }

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override fun getModality(): Modality {
        return Modality.FINAL
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        return CallableMemberDescriptor.Kind.DECLARATION
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {

    }

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? {
        return null
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): CallableMemberDescriptor = this

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out CallableMemberDescriptor> = TODO(
        "Not yet implemented"
    )

    override val original: CallableMemberDescriptor = this
    override val containingDeclaration: DeclarationDescriptor = this
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    override val name: Name
        get() = entry.name
    override val source: SourceElement
        get() = entry.source


    override fun toString(): String {
        return entry.toString()
    }
}

class EnumEntryDescriptor(
    c: LazyClassContext,
    thisDescriptor: DeclarationDescriptor,
    name: Name,
    info: CjEnmuEntryInfo,

    ) : LazyClassDescriptor(c, thisDescriptor, name, info, false) {


    override val classLikeInfo: CjEnmuEntryInfo
        get() = super.classLikeInfo as CjEnmuEntryInfo

    //    是否有无参构造
    fun hasUnsubstitutedPrimaryConstructor(): Boolean {
        return unsubstitutedPrimaryConstructor.valueParameters.isEmpty() /*&&
                containingDeclaration is LazyEnumDescriptor &&
                (containingDeclaration as LazyEnumDescriptor).declaredTypeParameters.isEmpty()*/

    }


    override val isStatic: Boolean
        get() = true
    override val visibility: DescriptorVisibility = containingDeclaration.visibility
    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptySet()

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor {
        return (unsubstitutedMemberScope as LazyClassMemberScope).getEnumEntryPrimaryConstructor()!!

    }

    fun toCallableMemberDescriptor(): EnumEntryCallableMemberDescriptor {
        return EnumEntryCallableMemberDescriptor(this)
    }

    override val constructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

}
