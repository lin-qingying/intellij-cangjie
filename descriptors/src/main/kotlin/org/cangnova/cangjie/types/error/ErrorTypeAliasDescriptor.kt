package org.cangnova.cangjie.types.error

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.TypeAliasConstructorDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.AbstractTypeAliasDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeSubstitutor

class ErrorTypeAliasDescriptor : AbstractTypeAliasDescriptor(
    LockBasedStorageManager.NO_LOCKS, ErrorUtils.errorModule,
    name = Name.ERROR_NAME,
    sourceElement = SourceElement.NO_SOURCE,
    visibilityImpl = DescriptorVisibilities.PUBLIC
) {
    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> {
        return emptyList()
    }

    override val underlyingType: SimpleType
        get() = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS)
    override val expandedType: SimpleType
        get() = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS)
    override val classDescriptor: ClassDescriptor?
        get() = null
    override val defaultType: SimpleType
        get() = ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE_ALIAS)

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        return null
    }

}