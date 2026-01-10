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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import java.io.File
import kotlin.io.path.absolutePathString

/**
 * 内置序列化器（Flatbuffers格式）
 *
 * 提供对CangJie标准库和内置类型的序列化数据访问
 */

object BuiltInSerializerFlatbuffers : SerializerExtensionFlatbuffers() {

    /**
     * 获取SDK模块路径前缀
     *
     * @return 模块路径前缀，如果SDK未配置则返回null
     */
    private fun getPrefix(sdk: CjSdk? = null): String? {


        val modulePath =
            sdk?.stdlibPath ?: CjSdkRegistry.getInstance().getAllSdks().firstOrNull()?.stdlibPath ?: return null



        return modulePath.absolutePathString() + File.separator
    }

    /**
     * 获取内置类型的序列化文件路径
     *
     * 尝试多种可能的路径：
     * 1. 使用父包作为目录: std/collection/std.collection.concurrent.cjo
     * 2. 使用顶级包作为目录: std/std.collection.concurrent.cjo
     * 3. 在SDK目录下搜索文件名
     *
     * @param fqName 完全限定名
     * @param sdk SDK实例
     * @return 序列化文件路径，如果SDK未配置或文件不存在则返回null
     */
    fun getBuiltInsFilePath(fqName: FqName, sdk: CjSdk?): String? {
        val prefix = getPrefix(sdk) ?: return null
        val fileName = getBuiltInsFileName(fqName)

        // 尝试路径1: 使用父包作为目录 (例如: std/collection/std.collection.concurrent.cjo)
        val parentPath = fqName.parent().asString()
        val path1 = if (parentPath.isEmpty()) {
            prefix + fileName
        } else {
            prefix + parentPath.replace('.', File.separatorChar) + File.separator + fileName
        }

        if (File(path1).exists()) {
            return path1
        }

        // 尝试路径2: 使用顶级包作为目录 (例如: std/std.collection.concurrent.cjo)
        val segments = fqName.pathSegments()
        if (segments.isNotEmpty()) {
            val topLevelPackage = segments.first().asString()
            val path2 = prefix + topLevelPackage + File.separator + fileName

            if (File(path2).exists()) {
                return path2
            }
        }

        // 尝试路径3: 在SDK目录下搜索文件名
        val prefixDir = File(prefix)
        if (prefixDir.exists() && prefixDir.isDirectory) {
            val foundFile = searchFileRecursively(prefixDir, fileName)
            if (foundFile != null) {
                return foundFile.absolutePath
            }
        }

        // 都找不到，返回null
        return null
    }

    /**
     * 递归搜索文件
     *
     * @param directory 搜索的目录
     * @param fileName 要搜索的文件名
     * @return 找到的文件，如果未找到则返回null
     */
    private fun searchFileRecursively(directory: File, fileName: String): File? {
        val files = directory.listFiles() ?: return null

        for (file in files) {
            if (file.isFile && file.name == fileName) {
                return file
            }
            if (file.isDirectory) {
                val found = searchFileRecursively(file, fileName)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    /**
     * 获取内置类型的序列化文件名
     *
     * @param fqName 完全限定名
     * @return 序列化文件名
     */
    private fun getBuiltInsFileName(fqName: FqName): String =
        fqName.asString() + DOT_DEFAULT_EXTENSION


    private const val BUILTINS_FILE_EXTENSION = "cjo"
    private const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"


}