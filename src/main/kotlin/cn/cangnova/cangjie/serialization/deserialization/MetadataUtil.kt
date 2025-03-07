/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.serialization.deserialization
import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import cn.cangnova.cangjie.metadata.decompiler.CangJieMetadataFinder
import cn.cangnova.cangjie.metadata.deserialization.NameResolverImpl
import cn.cangnova.cangjie.name.ClassId
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
