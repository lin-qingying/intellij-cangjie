package com.huawei.cangjie.descriptors.impl.basic

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractClassDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorBasicImpl
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.ClassTypeConstructorImpl
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class BasicDescriptor(
    val basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    val storageManager: StorageManager,
    override val name: Name
) : AbstractClassDescriptor(
    storageManager, name
) {
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
//        TODO("Not yet implemented")

        return MemberScope.Empty
    }

    override fun getUnsubstitutedMemberScope(): MemberScope {
//        TODO("Not yet implemented")

        return MemberScope.Empty
    }

    override fun getSource(): SourceElement {
        TODO("Not yet implemented")
    }

    val typeConstructor = ClassTypeConstructorImpl(this, emptyList(), emptyList(), storageManager)
    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getVisibility(): DescriptorVisibility {
        TODO("Not yet implemented")
    }

    override fun getModality(): Modality {
        TODO("Not yet implemented")
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        TODO("Not yet implemented")
    }

    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getStaticScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getConstructors(): MutableCollection<ClassConstructorDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getKind(): ClassKind {
        TODO("Not yet implemented")
    }

    override fun isFun(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isValue(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getSealedSubclasses(): MutableCollection<ClassDescriptor> {
        TODO("Not yet implemented")
    }

    override val containingDeclaration: DeclarationDescriptor
        get() = this
    override val annotations: Annotations
        get() = TODO("Not yet implemented")


}