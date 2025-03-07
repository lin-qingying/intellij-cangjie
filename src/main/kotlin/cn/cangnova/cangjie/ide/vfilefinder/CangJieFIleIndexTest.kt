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

import cn.cangnova.cangjie.lang.CangJieFileType
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
        CangJieFileType.INSTANCE
    )

    override fun dependsOnFileContent(): Boolean = true
}
