package com.linqingying.cangjie.resolve.lazy.descriptors

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.AbstractTypeAliasDescriptor
import com.linqingying.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.NullableLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue
import com.linqingying.cangjie.types.*

class LazyTypeAliasDescriptor(
    storageManager: StorageManager,
    private val trace: BindingTrace,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations = Annotations.EMPTY,
    name: Name,
    sourceElement: SourceElement,
    visibility: DescriptorVisibility
) : AbstractTypeAliasDescriptor(storageManager, containingDeclaration, annotations, name, sourceElement, visibility),
    TypeAliasDescriptor {
    override val constructors: Collection<TypeAliasConstructorDescriptor> by storageManager.createLazyValue {
        getTypeAliasConstructors()
    }
    private val lazyTypeConstructorParameters =
        storageManager.createRecursionTolerantLazyValue({ this.computeConstructorTypeParameters() }, emptyList())
    private lateinit var underlyingTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var expandedTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var defaultTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var classDescriptorImpl: NullableLazyValue<ClassDescriptor>
    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> =
        lazyTypeConstructorParameters()

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: SimpleType,
        expandedType: SimpleType
    ) = initialize(
        declaredTypeParameters,
        storageManager.createLazyValue { underlyingType },
        storageManager.createLazyValue { expandedType }
    )

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        lazyUnderlyingType: NotNullLazyValue<SimpleType>,
        lazyExpandedType: NotNullLazyValue<SimpleType>
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingTypeImpl = lazyUnderlyingType
        this.expandedTypeImpl = lazyExpandedType
        this.defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
        this.classDescriptorImpl =
            storageManager.createRecursionTolerantNullableLazyValue({ computeClassDescriptor() }, null)
    }

    private fun computeClassDescriptor(): ClassDescriptor? {
        if (underlyingType.isError) return null
        return when (val underlyingTypeDescriptor = underlyingType.constructor.declarationDescriptor) {
            is ClassDescriptor -> underlyingTypeDescriptor
            is TypeAliasDescriptor -> underlyingTypeDescriptor.classDescriptor
            else -> null
        }
    }

    override val underlyingType: SimpleType get() = underlyingTypeImpl()
    override val expandedType: SimpleType get() = expandedTypeImpl()
    override val classDescriptor: ClassDescriptor? get() = classDescriptorImpl()
    override fun getDefaultType(): SimpleType = defaultTypeImpl()


    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = LazyTypeAliasDescriptor(
            storageManager, trace,
            containingDeclaration, annotations, name, source, visibility
        )
        substituted.initialize(declaredTypeParameters,
            storageManager.createLazyValue {
                substitutor.substitute(underlyingType, Variance.INVARIANT)!!.asSimpleType()
            },
            storageManager.createLazyValue {
                substitutor.substitute(expandedType, Variance.INVARIANT)!!.asSimpleType()
            }
        )
        return substituted
    }

    companion object {
        @JvmStatic
        fun create(
            storageManager: StorageManager,
            trace: BindingTrace,
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,
            name: Name,
            sourceElement: SourceElement,
            visibility: DescriptorVisibility
        ): LazyTypeAliasDescriptor =
            LazyTypeAliasDescriptor(
                storageManager, trace,
                containingDeclaration, annotations, name, sourceElement, visibility
            )
    }
}
