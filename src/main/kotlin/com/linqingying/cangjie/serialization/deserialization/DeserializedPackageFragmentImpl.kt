package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolverImpl
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import com.linqingying.cangjie.storage.StorageManager


abstract class DeserializedPackageFragmentImpl(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    private val metadataVersion: BinaryVersion,
    private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    protected val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    override val classDataFinder =
        ProtoBasedClassDataFinder(proto, nameResolver, metadataVersion) { containerSource ?: SourceElement.NO_SOURCE }

    // Temporary storage: until `initialize` is called
    private var _proto: ProtoBuf.PackageFragment? = proto
    private lateinit var _memberScope: MemberScope

    override fun initialize(components: DeserializationComponents) {
        val proto = _proto ?: error("Repeated call to DeserializedPackageFragmentImpl::initialize")
        _proto = null
        _memberScope = DeserializedPackageMemberScope(
            this, proto.`package`, nameResolver, metadataVersion, containerSource, components,
            "scope of $this"
        ) {
            classDataFinder.allClassIds.filter { classId ->
                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
            }.map { it.shortClassName }
        }

    }

    override fun getMemberScope(): MemberScope = _memberScope
}
