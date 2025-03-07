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

package cn.cangnova.cangjie.ide.vfilefinder

import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileContent
import cn.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import cn.cangnova.cangjie.metadata.BuiltInDefinitionFile
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.serialization.deserialization.MetadataPackageFragment
import cn.cangnova.cangjie.serialization.deserialization.MetadataPackageFragment.Companion.DOT_METADATA_FILE_EXTENSION
import cn.cangnova.cangjie.serialization.deserialization.getClassId


abstract class CangJieMetadataFileIndexBase(indexFunction: (ClassId) -> FqName) : CangJieFileIndexBase() {
    override fun getIndexer() = INDEXER

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(CangJieBuiltInFileType)

    override fun getVersion() = 2

    private val INDEXER = indexer { fileContent ->
        val classId = fileContent.classIdFromCangJieMetadata() ?: return@indexer null
        indexFunction(classId)
    }
}

internal fun FileContent.classIdFromCangJieMetadata(): ClassId? {
    val builtIns = readCangJieMetadataDefinition(this) as? BuiltInDefinitionFile ?: return null

    val singleClass = builtIns.proto.class_List.singleOrNull()
    if (singleClass != null) {
        return builtIns.nameResolver.getClassId(singleClass.fqName)
    }

    val facadeName = this.fileName.substringBeforeLast(DOT_METADATA_FILE_EXTENSION)
    return ClassId(builtIns.packageFqName, Name.identifier(facadeName))
}
