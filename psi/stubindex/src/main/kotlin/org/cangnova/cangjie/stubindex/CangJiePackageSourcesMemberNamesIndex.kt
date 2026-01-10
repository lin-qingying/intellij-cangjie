/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.stubindex

import com.intellij.util.indexing.*
import com.intellij.util.indexing.hints.FileTypeInputFilterPredicate
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import java.io.DataInput
import java.io.DataOutput


private   val CANGJIE_DOT_FILE_EXTENSION = ".${CangJieFileType.EXTENSION}"


class CangJiePackageSourcesMemberNamesIndex internal constructor() :
    FileBasedIndexExtension<String, Collection<String>>() {


    companion object {
        val NAME: ID<String, Collection<String>> =
            ID.create(CangJiePackageSourcesMemberNamesIndex::class.java.simpleName)
    }

    private val KEY_DESCRIPTOR = EnumeratorStringDescriptor()

    override fun getName() = NAME

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = KEY_DESCRIPTOR

    override fun getValueExternalizer() = StringSetExternalizer

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileTypeInputFilterPredicate(CangJieFileType.INSTANCE,

        )

    override fun getVersion(): Int = 2

    override fun getIndexer(): DataIndexer<String, Collection<String>, FileContent> =
        DataIndexer { inputData ->
            // Check if ".cj" file is marked as plain text
            if (inputData.fileType !is CangJieFileType) return@DataIndexer emptyMap()

            val cjFile = inputData.psiFile as? CjFile ?: return@DataIndexer emptyMap()
            val packageName = cjFile.packageDirective?.fqName?.asString() ?: ""

//            if (!cjFile.isScript()) {
            mapOf(packageName to cjFile.declarations.mapNotNullTo(hashSetOf(), CjDeclaration::getName))
//            } else {
//                mapOf(packageName to listOfNotNull(cjFile.script?.name).toHashSet())
//            }
        }

}

object StringSetExternalizer : DataExternalizer<Collection<String>> {
    private val ELEMENTS_SERIALIZER = EnumeratorStringDescriptor()

    override fun read(input: DataInput): Set<String> {
        val size = input.readInt()
        return hashSetOf<String>().apply {
            repeat(size) {
                add(ELEMENTS_SERIALIZER.read(input))
            }
        }
    }

    override fun save(output: DataOutput, value: Collection<String>) {
        output.writeInt(value.size)
        value.toSet().forEach { ELEMENTS_SERIALIZER.save(output, it) }
    }
}
