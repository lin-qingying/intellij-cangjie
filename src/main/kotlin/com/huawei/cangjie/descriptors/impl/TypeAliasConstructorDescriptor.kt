package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
interface DescriptorDerivedFromTypeAlias {
    val typeAliasDescriptor: TypeAliasDescriptor
}

interface TypeAliasConstructorDescriptor : ConstructorDescriptor, DescriptorDerivedFromTypeAlias {
    val underlyingConstructorDescriptor: ClassConstructorDescriptor


    override fun getReturnType(): CangJieType
    override val original: TypeAliasConstructorDescriptor




    override val containingDeclaration: TypeAliasDescriptor


    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor?

    val withDispatchReceiver: TypeAliasConstructorDescriptor?

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor
}