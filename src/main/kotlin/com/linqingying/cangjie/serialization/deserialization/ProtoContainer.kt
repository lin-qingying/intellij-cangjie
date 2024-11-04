package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.metadata.deserialization.TypeTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName


sealed class ProtoContainer(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val source: SourceElement?
) {
    class Class(
        val classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        source: SourceElement?,
        val outerClass: ProtoContainer.Class?
    ) : ProtoContainer(nameResolver, typeTable, source) {
        val classId: ClassId = nameResolver.getClassId(classProto.fqName)

        val kind: ProtoBuf.Class.Kind = Flags.CLASS_KIND.get(classProto.flags) ?: ProtoBuf.Class.Kind.CLASS


        override fun debugFqName(): FqName = classId.asSingleFqName()
    }

    class Package(
        val fqName: FqName,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        source: SourceElement?
    ) : ProtoContainer(nameResolver, typeTable, source) {
        override fun debugFqName(): FqName = fqName
    }

    abstract fun debugFqName(): FqName

    override fun toString() = "${this::class.java.simpleName}: ${debugFqName()}"
}
