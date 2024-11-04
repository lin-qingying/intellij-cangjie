
package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.serialization.MutableVersionRequirementTable
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.FlexibleType


abstract class SerializerExtension {
    abstract val stringTable: DescriptorAwareStringTable

    abstract val metadataVersion: BinaryVersion

    val annotationSerializer by lazy { createAnnotationSerializer() }

    protected open fun createAnnotationSerializer(): AnnotationSerializer = AnnotationSerializer(stringTable)

    open fun shouldUseTypeTable(): Boolean = false
    open fun shouldUseNormalizedVisibility(): Boolean = false

    interface ClassMembersProducer {
        fun getCallableMembers(classDescriptor: ClassDescriptor): Collection<CallableMemberDescriptor>
    }

    open val customClassMembersProducer: ClassMembersProducer?
        get() = null


    open fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
    }

    open fun serializeConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
    }
    open fun serializeVariable(
        descriptor: VariableDescriptor,
        proto: ProtoBuf.Variable.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
    }
    open fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
    }

    open fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
    }

    open fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
    }

    open fun serializeType(type: CangJieType, proto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
    }

    open fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
    }

    open fun serializeErrorType(type: CangJieType, builder: ProtoBuf.Type.Builder) {
        throw IllegalStateException("Cannot serialize error type: $type")
    }
}
