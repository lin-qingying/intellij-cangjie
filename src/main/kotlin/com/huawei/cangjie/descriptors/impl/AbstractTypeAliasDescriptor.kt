package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.builtIns
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.storage.getValue

abstract class AbstractTypeAliasDescriptor(
    protected val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations = Annotations.EMPTY,
    name: Name,
    sourceElement: SourceElement,
    private val visibilityImpl: DescriptorVisibility
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, sourceElement),
    TypeAliasDescriptor {
    override val constructors: Collection<TypeAliasConstructorDescriptor> by storageManager.createLazyValue {
        getTypeAliasConstructors()
    }

    // TODO cangjieize some interfaces
    private lateinit var declaredTypeParametersImpl: List<TypeParameterDescriptor>

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>) {
        this.declaredTypeParametersImpl = declaredTypeParameters
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitTypeAliasDescriptor(this, data)


//    override fun isInner(): Boolean =
//    // NB: it's ok to use underlyingType here, since referenced inner type aliases also capture type parameters.
//    // Using expandedType looks "proper", but in fact will cause a recursion in expandedType resolution,
//        // which will silently produce wrong result.
//        TypeUtils.contains(underlyingType) { type ->
//            !type.isError && run {
//                val constructorDescriptor = type.constructor.declarationDescriptor
//                constructorDescriptor is TypeParameterDescriptor &&
//                        constructorDescriptor.containingDeclaration != this@AbstractTypeAliasDescriptor
//            }
//        }


    fun getTypeAliasConstructors(): Collection<TypeAliasConstructorDescriptor> {
        val classDescriptor = this.classDescriptor ?: return emptyList()

        return classDescriptor.constructors.mapNotNull {
            TypeAliasConstructorDescriptorImpl.createIfAvailable(storageManager, this, it)
        }
    }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> =
        declaredTypeParametersImpl

    override fun getModality() = Modality.FINAL

    override val visibility: DescriptorVisibility
        get() = visibilityImpl
//    override fun isExpect(): Boolean = false
//
//    override fun isActual(): Boolean = false
//
//    override fun isExternal() = false

    override fun getTypeConstructor(): TypeConstructor =
        typeConstructor

    override fun toString(): String = "typealias ${name.asString()}"


    override val original: TypeAliasDescriptor
        get() = super.original as TypeAliasDescriptor

    protected abstract fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor>

    @OptIn(TypeRefinement::class)
    protected fun computeDefaultType(): SimpleType =
        TypeUtils.makeUnsubstitutedType(
            this,
            classDescriptor?.unsubstitutedMemberScope ?: MemberScope.Empty
        ) { cangjieTypeRefiner ->
            cangjieTypeRefiner?.refineDescriptor(this)?.defaultType
        }

    private val typeConstructor = object : TypeConstructor {
        override fun getDeclarationDescriptor(): TypeAliasDescriptor =
            this@AbstractTypeAliasDescriptor

        override fun getParameters(): List<TypeParameterDescriptor> =
            getTypeConstructorTypeParameters()

        override fun getSupertypes(): Collection<CangJieType> =
            declarationDescriptor.underlyingType.constructor.supertypes

//        override fun isFinal(): Boolean =
//            declarationDescriptor.underlyingType.constructor.isFinal

        override fun isDenotable(): Boolean =
            true

        override fun getBuiltIns(): CangJieBuiltIns =
            declarationDescriptor.builtIns

        override fun toString(): String = "[typealias ${declarationDescriptor.name.asString()}]"

        // There must be @TypeRefinement, but there is a bug with anonymous objects and experimental annotations
        // See KT-31728
        @OptIn(TypeRefinement::class)
        override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
    }
}

