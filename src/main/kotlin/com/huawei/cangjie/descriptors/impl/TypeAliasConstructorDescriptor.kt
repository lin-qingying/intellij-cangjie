package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.resolve.DescriptorFactory
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitContextReceiver
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.storage.getValue
import com.huawei.cangjie.types.*


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

class TypeAliasConstructorDescriptorImpl private constructor(
    val storageManager: StorageManager,
    override val typeAliasDescriptor: TypeAliasDescriptor,
    underlyingConstructorDescriptor: ClassConstructorDescriptor,
    original: TypeAliasConstructorDescriptor?,
    annotations: Annotations,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : FunctionDescriptorImpl(typeAliasDescriptor, original, annotations, SpecialNames.INIT, kind, source),
    TypeAliasConstructorDescriptor {
    override fun isPrimary(): Boolean =
        underlyingConstructorDescriptor.isPrimary

    override fun isEnd(): Boolean {
        return underlyingConstructorDescriptor.isEnd
    }

    override var underlyingConstructorDescriptor: ClassConstructorDescriptor = underlyingConstructorDescriptor
        private set

    override fun getReturnType(): CangJieType =
        super.getReturnType()!!

    override val original: TypeAliasConstructorDescriptor =
        super.original as TypeAliasConstructorDescriptor
    override val containingDeclaration: TypeAliasDescriptor =
        typeAliasDescriptor


    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor? {
        //    class C<T>(val x: T)
        //    typealias A<Q> = C<List<Q>>
        //
        //    val test = A(listOf(42))
        //
        // Here return type for substituted type alias constructor is 'C<List<Int>>',
        // which yields us a substitution [T -> List<Int>] that should be applied to
        // the unsubstituted underlying constructor with signature '<T> (T) -> C<T>'
        // producing substituted underlying constructor with signature '(List<Int>) -> C<List<Int>>'.
        val substitutedTypeAliasConstructor = super.substitute(substitutor) as TypeAliasConstructorDescriptorImpl
        val underlyingConstructorSubstitutor = TypeSubstitutor.create(substitutedTypeAliasConstructor.returnType)
        val substitutedUnderlyingConstructor =
            underlyingConstructorDescriptor.original.substitute(underlyingConstructorSubstitutor)
                ?: return null
        substitutedTypeAliasConstructor.underlyingConstructorDescriptor = substitutedUnderlyingConstructor
        return substitutedTypeAliasConstructor
    }


    // When resolution is ran for common calls, type aliases constructors are resolved as extensions
    // (i.e. after members, and with extension receiver)
    // But when resolving super-calls (with known set of candidates) constructors of inner classes are expected to have
    // a dispatch receiver
    override val withDispatchReceiver: TypeAliasConstructorDescriptor? by storageManager.createNullableLazyValue {
        TypeAliasConstructorDescriptorImpl(
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            underlyingConstructorDescriptor.annotations,
            underlyingConstructorDescriptor.kind,
            typeAliasDescriptor.source
        ).also { typeAliasConstructor ->
            val substitutorForUnderlyingClass =
                typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return@createNullableLazyValue null

            typeAliasConstructor.initialize(
                null,
                underlyingConstructorDescriptor.dispatchReceiverParameter?.substitute(substitutorForUnderlyingClass),
                underlyingConstructorDescriptor.contextReceiverParameters.map {
                    it.substitute(
                        substitutorForUnderlyingClass
                    )
                },
                typeAliasDescriptor.declaredTypeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                typeAliasDescriptor.visibility
            )
        }
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor =
        newCopyBuilder()
            .setOwner(newOwner)
            .setModality(modality)
            .setVisibility(visibility)
            .setKind(kind)
            .setCopyOverrides(copyOverrides)
            .build() as TypeAliasConstructorDescriptor

    override fun hasSynthesizedParameterNames(): Boolean = false


    override fun getConstructedClass(): ClassDescriptor =
        underlyingConstructorDescriptor.constructedClass

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        assert(kind == CallableMemberDescriptor.Kind.DECLARATION || kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            "Creating a type alias constructor that is not a declaration: \ncopy from: ${this}\nnewOwner: $newOwner\nkind: $kind"
        }
        assert(newName == null) { "Renaming type alias constructor: $this" }
        return TypeAliasConstructorDescriptorImpl(
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            annotations,
            CallableMemberDescriptor.Kind.DECLARATION, source
        )
    }

    companion object {
        private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
            if (classDescriptor == null) return null
            return TypeSubstitutor.create(expandedType)
        }

        fun createIfAvailable(
            storageManager: StorageManager,
            typeAliasDescriptor: TypeAliasDescriptor,
            constructor: ClassConstructorDescriptor
        ): TypeAliasConstructorDescriptor? {
            val substitutorForUnderlyingClass =
                typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return null
            val substitutedConstructor = constructor.substitute(substitutorForUnderlyingClass) ?: return null

            val typeAliasConstructor =
                TypeAliasConstructorDescriptorImpl(
                    storageManager, typeAliasDescriptor,
                    substitutedConstructor,
                    null, constructor.annotations,
                    constructor.kind, typeAliasDescriptor.source
                )

            val valueParameters =
                getSubstitutedValueParameters(
                    typeAliasConstructor, constructor.valueParameters, substitutorForUnderlyingClass
                ) ?: return null

            val returnType = substitutedConstructor.returnType.unwrap().lowerIfFlexible()
                .withAbbreviation(typeAliasDescriptor.defaultType)

            val receiverParameter = constructor.dispatchReceiverParameter?.let {
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    typeAliasConstructor,
                    substitutorForUnderlyingClass.safeSubstitute(it.type, Variance.INVARIANT),
                    Annotations.EMPTY
                )
            }

            val classDescriptor = typeAliasDescriptor.classDescriptor
            val contextReceiverParameters = classDescriptor?.let {
                constructor.contextReceiverParameters.mapIndexed { index, contextReceiver ->
                    DescriptorFactory.createContextReceiverParameterForClass(
                        classDescriptor,
                        substitutorForUnderlyingClass.safeSubstitute(contextReceiver.type, Variance.INVARIANT),
                        (contextReceiver.value as ImplicitContextReceiver).customLabelName,
                        Annotations.EMPTY,
                        index
                    )
                }
            } ?: emptyList()

            typeAliasConstructor.initialize(
                receiverParameter,
                null,
                contextReceiverParameters,
                typeAliasDescriptor.declaredTypeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                typeAliasDescriptor.visibility
            )

            return typeAliasConstructor
        }
    }
}
