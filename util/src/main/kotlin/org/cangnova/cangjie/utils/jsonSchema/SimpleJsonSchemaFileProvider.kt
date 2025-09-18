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
package org.cangnova.cangjie.utils.jsonSchema

import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import org.jetbrains.annotations.Nls

/**
 * 简单的 JsonSchemaFileProvider 。
 *
 * 必须确保 `jsonSchemaPath` 为有效的资源路径。
 *
 * @param fileName 匹配的文件名称
 * @param jsonSchemaPath schema文件资源路径
 *
 * @author <a href="mailto:yms_hi@Outlook.com" rel="nofollow">yms</a>
 * */
class SimpleJsonSchemaFileProvider(val fileName: String, val jsonSchemaPath: String,val schemaName:String = fileName) : JsonSchemaFileProvider{
    val _schemaFile: VirtualFile by lazy {
        val schemaUrl = this.javaClass.getResource(jsonSchemaPath)!! // 断言存在该文件
        val fileUrl = VfsUtil.convertFromUrl(schemaUrl)
        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)!!

        file.refresh(true,false){
            file.refresh(false,true)
        }

        file
    }

    override fun isAvailable(file: VirtualFile) = StringUtil.equalsIgnoreCase(fileName,file.name)

    override fun getName() = schemaName

    override fun getSchemaFile() = _schemaFile

    override fun isUserVisible() = false

    override fun getSchemaType() = SchemaType.schema

    override fun getPresentableName() = name

    override fun getSchemaVersion() = JsonSchemaVersion.SCHEMA_7 // 均使用 draft-07 schema
}