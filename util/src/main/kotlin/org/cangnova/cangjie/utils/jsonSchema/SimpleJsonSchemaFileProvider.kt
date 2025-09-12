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