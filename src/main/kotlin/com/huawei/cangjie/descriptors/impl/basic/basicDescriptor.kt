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
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class BasicTypeDescriptor(
    val basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    val storageManager: StorageManager,
    override val name: Name
) : AbstractClassDescriptor(
    storageManager, name
) {

    private var constructors: Set< ClassConstructorDescriptor>  = mutableSetOf()


    companion object{

        fun create(
            memberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
            storageManager: StorageManager,
            name: Name
        ): BasicTypeDescriptor {
            return BasicTypeDescriptor(
                memberScope,
                storageManager,
                name
            )
        }
    }

    fun initialize(

        constructors: Set< ClassConstructorDescriptor>,

    ) {

        this.constructors = constructors

    }
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {


        return basicMemberScope
    }

    override fun getUnsubstitutedMemberScope(): MemberScope {


        return basicMemberScope
    }

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE

    val typeConstructor = ClassTypeConstructorImpl(this, emptyList(), emptyList(), storageManager)
    override fun getTypeConstructor(): TypeConstructor = typeConstructor



    override fun getModality(): Modality = Modality.FINAL


    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> = mutableListOf()
    override fun getContextReceivers(): List<ReceiverParameterDescriptor>  = emptyList()

    override fun getStaticScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getConstructors(): Set<ClassConstructorDescriptor> {
        return constructors

    }



    override fun getKind(): ClassKind = ClassKind.BASIC
    override fun isFun(): Boolean = false

    override fun isValue(): Boolean = false

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getSealedSubclasses(): MutableCollection<ClassDescriptor> {
        TODO("Not yet implemented")
    }

    override val containingDeclaration: DeclarationDescriptor
        get() = this
    override val annotations: Annotations
        get() = Annotations.EMPTY


    override fun toString(): String {
        return name.asString()
    }

}
