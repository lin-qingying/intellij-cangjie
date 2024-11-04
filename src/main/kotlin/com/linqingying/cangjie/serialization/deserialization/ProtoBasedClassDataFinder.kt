package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.name.ClassId

class ProtoBasedClassDataFinder(
    proto: ProtoBuf.PackageFragment,
    private val nameResolver: NameResolver,
    private val metadataVersion: BinaryVersion,
    private val classSource: (ClassId) -> SourceElement = { SourceElement.NO_SOURCE }
) : ClassDataFinder {
    private val classIdToProto =
        proto.class_List.associateBy { cclass ->
            nameResolver.getClassId(cclass.fqName)
        }

    val allClassIds: Collection<ClassId> get() = classIdToProto.keys

    override fun findClassData(classId: ClassId): ClassData? {
        val classProto = classIdToProto[classId] ?: return null
        return ClassData(nameResolver, classProto, metadataVersion, classSource(classId))
    }
}
