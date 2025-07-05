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
import cn.cangnova.cangjie.metadata.deserialization.Flags
import cn.cangnova.cangjie.metadata.deserialization.NameResolver
import cn.cangnova.cangjie.metadata.deserialization.TypeTable
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName


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
        val outerClass: Class?
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
