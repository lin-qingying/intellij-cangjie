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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * CJO 文件加载器
 *
 * 负责从文件系统加载和解析 CJO/CJB 文件。
 *
 * @see PackageWrapper
 */
object CjoFileLoader {

    private val LOG = Logger.getInstance(CjoFileLoader::class.java)

    val SUPPORTED_EXTENSIONS = setOf("cjo")

    /**
     * 从 VirtualFile 加载包元数据
     */
    fun loadFromFile(virtualFile: VirtualFile): PackageWrapper? {
        if (!virtualFile.isValid) {
            LOG.warn("Invalid virtual file: ${virtualFile.path}")
            return null
        }

        if (!isSupportedFile(virtualFile)) {
            LOG.warn("Unsupported file type: ${virtualFile.extension}")
            return null
        }

        return try {
            val bytes = virtualFile.contentsToByteArray(false)
            loadFromBytes(bytes)
        } catch (e: Exception) {
            LOG.warn("Failed to load package from ${virtualFile.path}", e)
            null
        }
    }

    /**
     * 从字节数组加载包元数据
     */
    fun loadFromBytes(bytes: ByteArray): PackageWrapper? {
        return try {
            ByteArrayInputStream(bytes).use { stream ->
                stream.toFbPackage().packageWrapper
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse package from bytes", e)
            null
        }
    }

    /**
     * 从 InputStream 加载包元数据
     */
    fun loadFromStream(inputStream: InputStream): PackageWrapper? {
        return try {
            inputStream.use { stream ->
                stream.toFbPackage().packageWrapper
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse package from stream", e)
            null
        }
    }

    /**
     * 检查文件是否为支持的 CJO 文件类型
     */
    fun isSupportedFile(virtualFile: VirtualFile): Boolean {
        val extension = virtualFile.extension?.lowercase() ?: return false
        return extension in SUPPORTED_EXTENSIONS
    }
}
