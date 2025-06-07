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

import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lang.declarations.CangJieDeclarationsFileType
import cn.cangnova.cangjie.psi.CjEnumEntry
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjTreeVisitorVoid
import cn.cangnova.cangjie.psi.CjTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.intellij.util.indexing.*
import com.intellij.util.indexing.impl.CollectionDataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor


class CangJieShortClassNameFileIndex : FileBasedIndexExtension<String, Collection<String>>() {
    companion object {
        val NAME: ID<String, Collection<String>> = ID.create(CangJieShortClassNameFileIndex::class.java.simpleName)
    }

    override fun getName(): ID<String, Collection<String>> = NAME

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer() = CollectionDataExternalizer(EnumeratorStringDescriptor.INSTANCE)

    override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
        DefaultFileTypeSpecificInputFilter(
            CangJieFileType.INSTANCE,
            CangJieDeclarationsFileType,

            )

    override fun getVersion() = 4

    override fun traceKeyHashToVirtualFileMapping(): Boolean = true

    override fun getIndexer() = DataIndexer<String, Collection<String>, FileContent> { fileContent ->
        val map = hashMapOf<String, Collection<String>>()
        when (fileContent.fileType) {

//            CangJieDeclarationsFileType -> {
//                val builtins = readCangJieMetadataDefinition(fileContent)
//                if (builtins != null) {
//                    for (classProto in builtins.classesToDecompile) {
//                        val classId = builtins.nameResolver.getClassId(classProto.fqName)
//                        map[classId.shortClassName.asString()] = listOf(classId.asFqNameString())
//                    }
//                }
//            }
            is CangJieFileType -> {
                val cjFile = fileContent.psiFile as? CjFile ?: return@DataIndexer emptyMap()
                cjFile.acceptChildren(object : CjTreeVisitorVoid() {
                    override fun visitEnumEntry(enumEntry: CjEnumEntry) {
                        add(enumEntry.name, enumEntry.safeFqNameForLazyResolve()?.asString())
                        super.visitEnumEntry(enumEntry)
                    }


                    override fun visitTypeStatement(typeStatement: CjTypeStatement) {
                        add(typeStatement.name, typeStatement.fqName?.asString())

                        super.visitTypeStatement(typeStatement)
                    }

                    private fun add(name: String?, fqName: String?) {
                        if (name != null && fqName != null) {
                            val fqNames = map.getOrPut(name) {
                                ArrayList()
                            } as ArrayList
                            fqNames += fqName
                        }
                    }
                })
            }
        }
        map
    }
}
