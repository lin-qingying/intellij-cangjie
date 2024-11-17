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

package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.NameResolverImpl
import com.linqingying.cangjie.metadata.serialization.Interner
import com.linqingying.cangjie.metadata.serialization.StringTable
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.DescriptorUtils

open class SerializableStringTable : StringTable {

    private class FqNameProto(val fqName: ProtoBuf.QualifiedNameTable.QualifiedName.Builder) {
        override fun hashCode(): Int {
            var result = 13
            result = 31 * result + fqName.parentQualifiedName
            result = 31 * result + fqName.shortName
            result = 31 * result + fqName.kind.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is FqNameProto) return false

            val otherFqName = other.fqName
            return fqName.parentQualifiedName == otherFqName.parentQualifiedName
                    && fqName.shortName == otherFqName.shortName
                    && fqName.kind == otherFqName.kind
        }
    }

    private val strings = Interner<String>()
    private val qualifiedNames = Interner<FqNameProto>()

    fun addString(string: String) {
        strings.intern(string)
    }

    fun addQualifiedName(qualifiedName: ProtoBuf.QualifiedNameTable.QualifiedName) {
        qualifiedNames.intern(FqNameProto(qualifiedName.toBuilder()))
    }

    override fun getStringIndex(string: String): Int = strings.intern(string)

    override fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int =
        getClassIdIndex(ClassId.fromString(className, isLocal))

    fun getClassIdIndex(classId: ClassId): Int {
        val builder = ProtoBuf.QualifiedNameTable.QualifiedName.newBuilder()
        builder.kind = ProtoBuf.QualifiedNameTable.QualifiedName.Kind.CLASS

        builder.parentQualifiedName =
            classId.outerClassId?.let(this::getClassIdIndex)
                ?: getPackageFqNameIndex(classId.packageFqName)

        builder.shortName = getStringIndex(classId.shortClassName.asString())

        return qualifiedNames.intern(FqNameProto(builder))
    }

    override fun getPackageFqNameIndexByString(fqName: String): Int =
        getPackageFqNameIndex(FqName(fqName))

    fun getPackageFqNameIndex(fqName: FqName): Int {
        var result = -1
        for (segment in fqName.pathSegments()) {
            val builder = ProtoBuf.QualifiedNameTable.QualifiedName.newBuilder()
            builder.shortName = getStringIndex(segment.asString())
            if (result != -1) {
                builder.parentQualifiedName = result
            }
            result = qualifiedNames.intern(FqNameProto(builder))
        }
        return result
    }

    fun buildProto(): Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> {
        val strings = ProtoBuf.StringTable.newBuilder()
        for (simpleName in this.strings.allInternedObjects) {
            strings.addString(simpleName)
        }

        val qualifiedNames = ProtoBuf.QualifiedNameTable.newBuilder()
        for (fqName in this.qualifiedNames.allInternedObjects) {
            qualifiedNames.addQualifiedName(fqName.fqName)
        }

        return Pair(strings.build(), qualifiedNames.build())
    }
}

open class StringTableImpl : DescriptorAwareStringTable, SerializableStringTable() {

    override fun getQualifiedClassNameIndex(classId: ClassId): Int = getClassIdIndex(classId)

    override val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = false
}


class CangJieCodegenStringTable (nameResolver:  NameResolverImpl? = null): StringTableImpl() {
    override fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId =
        when (val container = descriptor.containingDeclaration) {
            is ClassifierDescriptorWithTypeParameters -> getLocalClassIdReplacement(container).createNestedClassId(descriptor.name)
            is PackageFragmentDescriptor -> {
                throw IllegalStateException("getLocalClassIdReplacement should only be called for local classes: $descriptor")
            }
            else -> {
                  super.getLocalClassIdReplacement(descriptor) ?:  throw IllegalStateException("getLocalClassIdReplacement should only be called for local classes: $descriptor")

//                val fqName = FqName(typeMapper.mapClass(descriptor).internalName.replace('/', '.'))
//                ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), isLocal = true)
            }
        }

    override val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = true
}

class ApproximatingStringTable : StringTableImpl() {
    override fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? {
        return if (DescriptorUtils.isLocal(descriptor)) {
            ClassId.topLevel(StandardNames.FqNames.anyUFqName.toSafe())
        } else {
            super.getLocalClassIdReplacement(descriptor)
        }
    }

    override val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = false
}
