package com.huawei.cangjie.serialization.deserialization

import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import com.huawei.cangjie.storage.StorageManager


abstract class DeserializedPackageFragmentImpl(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
//    proto: ProtoBuf.PackageFragment,
//    private val metadataVersion: BinaryVersion,
//    private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    private lateinit var _memberScope: MemberScope
    override fun initialize(components: DeserializationComponents) {
//        val proto = _proto ?: error("Repeated call to DeserializedPackageFragmentImpl::initialize")
//        _proto = null
//        _memberScope = DeserializedPackageMemberScope(
//            this, proto.`package`, nameResolver, metadataVersion, containerSource, components,
//            "scope of $this"
//        ) {
//            classDataFinder.allClassIds.filter { classId ->
//                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
//            }.map { it.shortClassName }
//        }

    }

    override fun getMemberScope(): MemberScope = _memberScope

}
