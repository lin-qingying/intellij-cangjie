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

package cn.cangnova.cangjie.metadata.deserialization

import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.ProtoBuf.QualifiedNameTable.QualifiedName
import java.util.*

class NameResolverImpl(
    val strings: ProtoBuf.StringTable,
    val qualifiedNames: ProtoBuf.QualifiedNameTable
) : NameResolver {
    override fun getString(index: Int): String = strings.getString(index)

    override fun getQualifiedClassName(index: Int): String {
        val (packageFqNameSegments, relativeClassNameSegments) = traverseIds(index)
        val className = relativeClassNameSegments.joinToString(".")
        return if (packageFqNameSegments.isEmpty()) className
        else packageFqNameSegments.joinToString("/") + "/$className"
    }

    override fun isLocalClassName(index: Int): Boolean =
        traverseIds(index).third

    fun getPackageFqName(index: Int): String =
        traverseIds(index).first.joinToString(".")

    private fun traverseIds(startingIndex: Int): Triple<List<String>, List<String>, Boolean> {
        var index = startingIndex
        val packageNameSegments = LinkedList<String>()
        val relativeClassNameSegments = LinkedList<String>()
        var local = false

        while (index != -1) {
            val proto = qualifiedNames.getQualifiedName(index)
            val shortName = strings.getString(proto.shortName)
            when (proto.kind!!) {
                QualifiedName.Kind.CLASS -> relativeClassNameSegments.addFirst(shortName)
                QualifiedName.Kind.PACKAGE -> packageNameSegments.addFirst(shortName)
                QualifiedName.Kind.LOCAL -> {
                    relativeClassNameSegments.addFirst(shortName)
                    local = true
                }
            }

            index = proto.parentQualifiedName
        }
        return Triple(packageNameSegments, relativeClassNameSegments, local)
    }
}
