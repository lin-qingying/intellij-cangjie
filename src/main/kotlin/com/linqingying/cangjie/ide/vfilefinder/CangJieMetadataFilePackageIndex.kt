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

package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.ID
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType
import com.linqingying.cangjie.metadata.BuiltInDefinitionFile
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName


class CangJieMetadataFileIndex :CangJieMetadataFileIndexBase(ClassId::asSingleFqName) {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieMetadataFileIndex")
    }

    override fun getName() = NAME
}

class CangJieMetadataFilePackageIndex : CangJieMetadataFileIndexBase(ClassId::packageFqName) {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieMetadataFilePackageIndex")
    }

    override fun getName() = NAME
}
class CangJieBuiltInsMetadataIndex : CangJieFileIndexBase() {
    companion object {
        val NAME: ID<FqName, Void> = ID.create("com.linqingying.cangjie.ide.vfilefinder.CangJieBuiltInsMetadataIndex")
    }

    override fun getName() = NAME

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(CangJieBuiltInFileType)

    override fun getVersion() = VERSION

    private val VERSION = 4

    private val INDEXER = indexer { fileContent ->
        val packageFqName =
            if (fileContent.fileType == CangJieBuiltInFileType  )
             {
                val builtins = readCangJieMetadataDefinition(fileContent) as? BuiltInDefinitionFile
                builtins?.packageFqName
            } else null
        packageFqName
    }
}
