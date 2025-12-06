/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.stubindex


import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationsFileType
import org.cangnova.cangjie.lang.declarations.CjDeclarationsFile
import org.cangnova.cangjie.name.parentOrNull
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.safeAs
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput
import org.cangnova.cangjie.name.*

private val LOG = logger<CangJiePartialPackageNamesIndex>()
val NAME: ID<FqName, Name?> = ID.create(CangJiePartialPackageNamesIndex::class.java.simpleName)

data class NameIsRoot(
    val name: Name,
    val isRoot: Boolean
) {
    fun asString() = "$name:$isRoot"
}

internal class CangJiePartialPackageNamesIndex : FileBasedIndexExtension<FqName, Name?>() {


    private object NullableNameExternalizer : DataExternalizer<Name?> {
        override fun save(out: DataOutput, value: Name?) {
            out.writeBoolean(value == null)
            if (value != null) {
                IOUtil.writeUTF(out, value.asString())
            }
        }

        override fun read(input: DataInput): Name? =
            if (input.readBoolean()) null else Name.guessByFirstCharacter(IOUtil.readUTF(input))


        fun guessByFirstCharacter(s: String): NameIsRoot {
            val (name, isRoot) = s.split(":")

            return NameIsRoot(Name.guessByFirstCharacter(name), isRoot.toBoolean())

        }
    }


    override fun getName() = NAME

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = FqNameKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<Name?> = NullableNameExternalizer

    override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
        DefaultFileTypeSpecificInputFilter(
//            JavaClassFileType.INSTANCE,
            CangJieFileType.INSTANCE,
            CangJieDeclarationsFileType

//            CangJieJavaScriptMetaFileType,

//            KlibMetaFileType,
        )

    override fun getVersion() = 3

    override fun traceKeyHashToVirtualFileMapping(): Boolean = true
    private fun FileContent.getBuiltInFilePackage(): FqName? {
        assert(this.fileType == CangJieDeclarationsFileType)
        val virtualFile =
            FileTypeIndex.getFiles(fileType, GlobalSearchScope.projectScope(project)).firstOrNull()
        val psiFIle = virtualFile?.let { PsiManager.getInstance(project).findFile(it) }
        return psiFIle.safeAs<CjDeclarationsFile>()?.packageFqName
    }

    private fun FileContent.toPackageFqName(): FqName? =
        when (this.fileType) {
            CangJieFileType.INSTANCE -> this.psiFile.safeAs<CjFile>()?.packageFqName

            CangJieDeclarationsFileType ->/* this.getBuiltInFilePackage()*/this.psiFile.safeAs<CjDeclarationsFile>()?.packageFqName

            else -> null
        }


    override fun getIndexer() = DataIndexer<FqName, Name?, FileContent> { fileContent ->
        try {
            val packageFqName = fileContent.toPackageFqName() ?: return@DataIndexer emptyMap<FqName, Name?>()

            val a = generateSequence(packageFqName) {
                it.parentOrNull()
            }
            val b = a.filterNot { it.isRoot }
            val c = b.associateBy({ it.parent() }, {

                it.shortName().apply {
                    isRoot = it.parent().isRoot
                }

            })
            val d = c + mapOf(packageFqName to null)
            d

        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            LOG.warn("Error `(${e.javaClass.simpleName}: ${e.message})` while indexing file ${fileContent.fileName} using $name index. Probably the file is broken.")
            emptyMap()
        }
    }
}

