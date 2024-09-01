package com.huawei.cangjie.descriptors.impl.basic

import com.huawei.cangjie.builtins.StandardNames.FqNames.core
import com.huawei.cangjie.builtins.StandardNames.FqNames.ctypeFqName
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractClassDescriptor
import com.huawei.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjSuperTypeListEntry
import com.huawei.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorBasicImpl
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

class BuiltInTypeDescriptor(
    basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    storageManager: StorageManager,
    name: Name,
    private val parameters: List<TypeParameterDescriptor> = emptyList(),
//    如果后期有其他内置类型有泛型，可以在这里添加回调函数，目前只有CPointer有泛型，所以不做更改
) : BasicTypeDescriptor(basicMemberScope, storageManager, name) {


    override val typeConstructor = BuiltInTypeConstructor(this, storageManager, parameters.toMutableList())


    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> {

        return typeConstructor.parameters.map {
            it.apply {
                this as TypeParameterDescriptorImpl
                if( !isInitialized()) {
                    findCangJieTypeByFqName(storageManager.project, ctypeFqName)?.let { addUpperBound(it) }
                    setInitialized()
                }

            }
        }.toMutableList()
    }

    fun addParameter(parameters: TypeParameterDescriptor) {

        typeConstructor.addParameter(parameters)
    }

//    override val visibility: DescriptorVisibility
//        get() = DescriptorVisibilities.LOCAL

    val packageView = EmptyDeclarationDescriptor()

    inner class EmptyDeclarationDescriptor : DeclarationDescriptor {
        override val original: DeclarationDescriptor
            get() = this

        override val containingDeclaration: DeclarationDescriptor?
            get() = getPackageView(storageManager.project, core)

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
            return null
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

        }

        override val name: Name = Name.identifier("EmptyDeclarationDescriptor")

    }

    override val containingDeclaration: DeclarationDescriptor
        get() = packageView

    companion object {

        fun create(
            memberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
            storageManager: StorageManager,
            name: Name,
            parameters: List<TypeParameterDescriptor> = emptyList()
        ): BuiltInTypeDescriptor {
            return BuiltInTypeDescriptor(
                memberScope,
                storageManager,
                name,
                parameters.toMutableList()
            )
        }
    }
}

open class BasicTypeDescriptor(
    val basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    val storageManager: StorageManager,
    override val name: Name
) : AbstractClassDescriptor(
    storageManager, name
), ClassDescriptorWithResolutionScopes {

    val extendClassDescriptor = mutableSetOf<LazyExtendClassDescriptor>()

    private var constructors: Set<ClassConstructorDescriptor> = mutableSetOf()

    //    override fun getDefaultType(): BasicType {
//
//
//        return basicMemberScope.getContributedClassifier(name, NoLookupLocation.FROM_BUILTINS)
//    }
//    基本类型返回扩展
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> {
        val result = mutableListOf<CjSuperTypeListEntry>()
        extendClassDescriptor.forEach {
            result.addAll(it.typeStatement.superTypeListEntries)
        }

        return result
    }

    companion object {

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

        constructors: Set<ClassConstructorDescriptor>,

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
    override fun getDefaultType(): BasicType {
        return BasicType(typeConstructor, basicMemberScope)
    }

    //    val typeConstructor = ClassTypeConstructorImpl(this, emptyList(), emptyList(), storageManager)
    open val typeConstructor = BasicTypeConstructor(this, storageManager)
    override fun getTypeConstructor(): TypeConstructor = typeConstructor


    override fun getModality(): Modality = Modality.FINAL
    override fun setModality(modality: Modality) {

    }


    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> = mutableListOf()
    override fun getContextReceivers(): List<ReceiverParameterDescriptor> = emptyList()

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
        return null
    }

    override fun getSealedSubclasses(): List<ClassDescriptor> {
        return emptyList()
    }

    override fun getScopeForMemberDeclarationResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> {
        return emptyList()
    }

    override fun getScopeForInitializerResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getScopeForClassHeaderResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getScopeForConstructorHeaderResolution(): LexicalScope {
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
