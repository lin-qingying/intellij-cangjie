package com.huawei.cangjie.ide.vfilefinder

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.lang.declarations.CangJieBuiltInFileType
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjTreeVisitorVoid
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.intellij.util.indexing.*
import com.intellij.util.indexing.impl.CollectionDataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor


class CangJieShortClassNameFileIndex : FileBasedIndexExtension<String, Collection<String>>() {
    companion object {
        val NAME: ID<String, Collection<String>> = ID.create(CangJieShortClassNameFileIndex::class.java.canonicalName)
    }

    override fun getName(): ID<String, Collection<String>> = NAME

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer() = CollectionDataExternalizer(EnumeratorStringDescriptor.INSTANCE)

    override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
        DefaultFileTypeSpecificInputFilter(
            CangJieFileType.INSTANCE,
            CangJieBuiltInFileType,

            )

    override fun getVersion() = 4

    override fun traceKeyHashToVirtualFileMapping(): Boolean = true

    override fun getIndexer() = DataIndexer<String, Collection<String>, FileContent> { fileContent ->
        val map = hashMapOf<String, Collection<String>>()
        when (fileContent.fileType) {

//            CangJieBuiltInFileType -> {
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
