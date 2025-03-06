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

package cn.cangnova.cangjie.ide.stubindex

import cn.cangnova.cangjie.name.FqName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor

import java.io.DataInput
import java.io.DataOutput
import java.util.*

fun hasSomethingInPackage(indexId: ID<FqName, Void>, fqName: FqName, scope: GlobalSearchScope): Boolean {
    return DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
        !FileBasedIndex.getInstance().processValues(indexId, fqName, null, { _, _ -> false }, scope)
    })
}


object FqNameKeyDescriptor : KeyDescriptor<FqName> {
    override fun save(output: DataOutput, value: FqName) = IOUtil.writeUTF(output, value.asString())
    override fun read(input: DataInput) = FqName(IOUtil.readUTF(input))
    override fun getHashCode(value: FqName) = value.asString().hashCode()
    override fun isEqual(val1: FqName?, val2: FqName?) = val1 == val2
}


abstract class CangJieFileIndexBase : ScalarIndexExtension<FqName>() {
    protected val LOG = Logger.getInstance(javaClass)

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = FqNameKeyDescriptor

    protected fun indexer(f: (FileContent) -> FqName?): DataIndexer<FqName, Void, FileContent> {
        return DataIndexer {
            try {
                val fqName = f(it)
                if (fqName != null) {
                    Collections.singletonMap<FqName, Void>(fqName, null)
                } else {
                    emptyMap()
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                LOG.warn("Error while indexing file ${it.fileName}: ${e.message}")
                emptyMap()
            }
        }
    }
}
//
//class CangJieClassFileIndex : CangJieFileIndexBase() {
//    companion object {
//        val NAME: ID<FqName, Void> = ID.create("cn.cangnova.cangjie.idea.vfilefinder.CangJieClassFileIndex")
//    }
//
//    override fun getName() = NAME
//
//    override fun getIndexer() = INDEXER
//
//    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(JavaClassFileType.INSTANCE)
//
//    override fun getVersion() = 3
//
//    private val INDEXER = indexer { fileContent ->
//        val headerInfo = ClsCangJieBinaryClassCache.getInstance().getCangJieBinaryClassHeaderData(fileContent.file, fileContent.content)
//        if (headerInfo != null && headerInfo.metadataVersion.isCompatible()) headerInfo.classId.asSingleFqName() else null
//    }
//}
//
//class CangJieStdlibIndex : CangJieFileIndexBase() {
//    companion object {
//        val NAME: ID<FqName, Void> = ID.create("cn.cangnova.cangjie.idea.vfilefinder.CangJieStdlibIndex")
//
//        val CANGJIE_STDLIB_NAME: FqName = FqName("cangjie-stdlib")
//        val STANDARD_LIBRARY_DEPENDENCY_NAME: FqName = FqName("cangjie-stdlib-common")
//
//        private const val LIBRARY_NAME_MANIFEST_ATTRIBUTE = "Implementation-Title"
//        private const val STDLIB_TAG_MANIFEST_ATTRIBUTE = "CangJie-Runtime-Component"
//    }
//
//    override fun getName() = NAME
//
//    override fun getIndexer() = INDEXER
//
//    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(ManifestFileType.INSTANCE)
//
//    override fun getVersion() = 1
//
//    // TODO: refactor [CangJieFileIndexBase] and get rid of FqName here, it's never a proper fully qualified name, just a String wrapper
//    private val INDEXER = indexer { fileContent ->
//        if (fileContent.fileType is ManifestFileType) {
//            val manifest = Manifest(ByteArrayInputStream(fileContent.content))
//            val attributes = manifest.mainAttributes
//            attributes.getValue(STDLIB_TAG_MANIFEST_ATTRIBUTE) ?: return@indexer null
//            val libraryName = attributes.getValue(LIBRARY_NAME_MANIFEST_ATTRIBUTE) ?: return@indexer null
//            FqName(libraryName)
//        } else null
//    }
//}
