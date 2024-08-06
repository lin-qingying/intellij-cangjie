package com.huawei.cangjie.ide.vfilefinder

import com.huawei.cangjie.lang.CangJieFileType
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class CangJieFIleIndexTest : FileBasedIndexExtension<String, String>() {
    override fun getName(): ID<String, String>  = NAME
    companion object {
        val NAME: ID<String,String> = ID.create(CangJieFIleIndexTest::class.java.canonicalName)
    }
    override fun getIndexer(): DataIndexer<String, String, FileContent> {


        return DataIndexer<String, String, FileContent> { inputData -> mapOf("CangJieFileIndexTest" to inputData.file.name).toMutableMap() }
    }

    private object NullableNameExternalizer : DataExternalizer<String> {
        override fun save(out: DataOutput, value: String) {
//            out.writeBoolean(value == null)
//            if (value != null) {
                IOUtil.writeUTF(out, value)
//            }
        }

        override fun read(input: DataInput): String? {
//            return if (!input.readBoolean()) {
//                null
//            } else {
//                IOUtil.readUTF(input)
//            }
            return IOUtil.readUTF(input)
        }

    }

    object StringKeyDescriptor : KeyDescriptor<String> {
        override fun save(output: DataOutput, value: String) = IOUtil.writeUTF(output, value)
        override fun read(input: DataInput): String? = IOUtil.readUTF(input)
        override fun getHashCode(value: String) = value.hashCode()
        override fun isEqual(val1: String?, val2: String?) = val1 == val2
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = StringKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<String> = NullableNameExternalizer

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(
        CangJieFileType
    )

    override fun dependsOnFileContent(): Boolean = true
}
