/*
 * Copyright 2010-2023 JetBrains s.r.o. and CangJie Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.serialization.deserialization
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import com.linqingying.cangjie.metadata.decompiler.CangJieMetadataFinder
import com.linqingying.cangjie.metadata.deserialization.NameResolverImpl
import com.linqingying.cangjie.name.ClassId
import java.io.InputStream
const val METADATA_FILE_EXTENSION = "cangjie_metadata"
const val DOT_METADATA_FILE_EXTENSION = ".$METADATA_FILE_EXTENSION"

class MetadataClassDataFinder(val finder: CangJieMetadataFinder) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val topLevelClassId = generateSequence(classId, ClassId::outerClassId).last()
        val stream = finder.findMetadata(topLevelClassId) ?: return null
        val (message, nameResolver, version) = readProto(stream)
        return message.class_List.firstOrNull { classProto ->
            nameResolver.getClassId(classProto.fqName) == classId
        }?.let { classProto ->
            ClassData(nameResolver, classProto, version, SourceElement.NO_SOURCE)
        }
    }
}

fun readProto(stream: InputStream): Triple<ProtoBuf.PackageFragment, NameResolverImpl, BuiltInsBinaryVersion> {
    val version = BuiltInsBinaryVersion.readFrom(stream)

    if (!version.isCompatibleWithCurrentCompilerVersion()) {
        // TODO: report a proper diagnostic
        throw UnsupportedOperationException(
            "CangJie metadata definition format version is not supported: " +
                    "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                    "Please update CangJie"
        )
    }

    val message = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
    val nameResolver = NameResolverImpl(message.strings, message.qualifiedNames)
    return Triple(message, nameResolver, version)
}
