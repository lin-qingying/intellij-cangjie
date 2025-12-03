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
     * @param fqName 完全限定名
     * @return 序列化文件路径，如果SDK未配置则返回null
     */
    fun getBuiltInsFilePath(fqName: FqName, sdk: CjSdk?): String? {
        val prefix = getPrefix(sdk) ?: return null
        return prefix + fqName.parent().asString() + if (fqName.parent().asString().isEmpty()) {
            ""
        } else {
            File.separator
        } + getBuiltInsFileName(fqName)
    }

    /**
     * 获取内置类型的序列化文件名
     *
     * @param fqName 完全限定名
     * @return 序列化文件名
     */
    fun getBuiltInsFileName(fqName: FqName): String =
        fqName.asString() + DOT_DEFAULT_EXTENSION


    const val BUILTINS_FILE_EXTENSION = "cjo"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"


}