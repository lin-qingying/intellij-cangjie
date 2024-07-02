package com.huawei.cangjie.idea.vfilefinder

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile
import com.intellij.util.indexing.*
import com.intellij.util.indexing.hints.FileTypeInputFilterPredicate
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import java.io.DataInput
import java.io.DataOutput


private const val KOTLIN_DOT_FILE_EXTENSION = ".${CangJieFileType.EXTENSION}"


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
        FileTypeInputFilterPredicate(CangJieFileType)

    override fun getVersion(): Int = 2

    override fun getIndexer(): DataIndexer<String, Collection<String>, FileContent> =
        DataIndexer { inputData ->
            // Check if ".kt" file is marked as plain text
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
