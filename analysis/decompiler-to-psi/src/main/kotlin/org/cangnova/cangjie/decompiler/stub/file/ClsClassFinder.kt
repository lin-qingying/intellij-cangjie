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

package org.cangnova.cangjie.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.lang.CangJieMetadataFileType
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

/**
 * 用于查找和判断仓颉编译文件类型的工具类。
 *
 * 提供以下功能：
 * - 判断文件是否为内部编译文件（不应显示给用户）
 * - 查找多文件类的各个部分
 * - 管理多文件类部分的处理策略
 */
object ClsClassFinder {

    private val LOG = Logger.getInstance(ClsClassFinder::class.java)

    /**
     * 查找多文件类的所有部分。
     *
     * @param file 多文件类的主文件
     * @param classId 类的标识符
     * @param partNames 部分文件的名称列表
     * @return 找到的二进制类列表
     */
    fun findMultifileClassParts(
        file: VirtualFile,
        classId: ClassId,
        partNames: List<String>
    ): List<CangJieBinaryClass> {
        val packageFqName = classId.packageFqName
        val parent = file.parent ?: return emptyList()
        val partsFinder = DirectoryBasedClassFinder(parent, packageFqName)

        return partNames.mapNotNull { partName ->
            val partClassId = ClassId(packageFqName, Name.identifier(partName.substringAfterLast('/')))
            partsFinder.findCangJieClass(partClassId, BinaryVersion.INSTANCE)
        }
    }

    /**
     * 检查文件是否为内部编译的仓颉文件。
     *
     * 内部文件不应被反编译、显示在项目视图中或通过 "查找类" 搜索到。
     *
     * @param file 要检查的虚拟文件
     * @param fileContent 文件内容（可选，用于避免重复读取）
     * @param multifileClassPartKindStrategy 多文件类部分的处理策略
     * @return 如果是内部文件返回 true
     */
    @JvmOverloads
    fun isCangJieInternalCompiledFile(
        file: VirtualFile,
        fileContent: ByteArray? = null,
        multifileClassPartKindStrategy: MultifileClassPartKindStrategy = MultifileClassPartKindStrategy.FROM_STACK,
    ): Boolean {
        if (!file.isValidAndExists(fileContent)) {
            return false
        }

        val cache = ClsCangJieBinaryClassCache.getInstance()

        if (!cache.isCangJieCompiledFile(file, fileContent)) {
            return false
        }

        // 检查是否为嵌套类
        val isNestedClass = try {
            isNestedClassFile(file, fileContent)
        } catch (exception: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(exception)
            LOG.debug("Error checking nested class: ${file.path}", exception)
            return false
        }

        if (isNestedClass) {
            return true
        }

        val header = cache.getCangJieBinaryClassHeaderData(file, fileContent) ?: return false

        // 检查是否为本地类
        if (header.classId.isLocal) {
            return true
        }

        return when (header.kind) {
            CangJieClassHeader.Kind.SYNTHETIC_CLASS,
            CangJieClassHeader.Kind.UNKNOWN -> true

            CangJieClassHeader.Kind.MULTIFILE_CLASS_PART -> when (multifileClassPartKindStrategy) {
                MultifileClassPartKindStrategy.INTERNAL -> true
                MultifileClassPartKindStrategy.NON_INTERNAL -> false
                MultifileClassPartKindStrategy.FROM_STACK -> treatMultifileClassPartAsInternal.get()
            }

            CangJieClassHeader.Kind.CLASS,
            CangJieClassHeader.Kind.FILE_FACADE,
            CangJieClassHeader.Kind.MULTIFILE_CLASS -> false
        }
    }

    /**
     * 多文件类部分的处理策略。
     *
     * @see isCangJieInternalCompiledFile
     */
    enum class MultifileClassPartKindStrategy {
        /**
         * 视为内部文件。
         */
        INTERNAL,

        /**
         * 视为普通文件外观（FILE_FACADE）。
         */
        NON_INTERNAL,

        /**
         * 根据线程局部标志决定，默认视为内部文件。
         *
         * @see allowMultifileClassPart
         */
        FROM_STACK
    }

    /**
     * 在执行期间允许多文件类部分作为非内部文件。
     *
     * @param action 要执行的操作
     * @return 操作的返回值
     * @see MultifileClassPartKindStrategy.FROM_STACK
     */
    fun <T> allowMultifileClassPart(action: () -> T): T {
        val old = treatMultifileClassPartAsInternal.get()
        return try {
            treatMultifileClassPartAsInternal.set(false)
            action()
        } finally {
            treatMultifileClassPartAsInternal.set(old)
        }
    }

    private val treatMultifileClassPartAsInternal = ThreadLocal.withInitial { true }

    /**
     * 检查文件是否为多文件类的部分。
     *
     * @param file 要检查的虚拟文件
     * @param fileContent 文件内容（可选）
     * @return 如果是多文件类部分返回 true
     */
    fun isMultifileClassPartFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        if (!file.isValidAndExists(fileContent)) {
            return false
        }
        val cache = ClsCangJieBinaryClassCache.getInstance()
        val headerData = cache.getCangJieBinaryClassHeaderData(file, fileContent)
        return headerData?.kind == CangJieClassHeader.Kind.MULTIFILE_CLASS_PART
    }

    /**
     * 检查文件是否为嵌套类文件。
     *
     * @param file 虚拟文件
     * @param fileContent 文件内容（可选）
     * @return 如果是嵌套类返回 true
     */
    private fun isNestedClassFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        // 通过文件名判断：包含 '$' 的通常是嵌套类
        val fileName = file.nameWithoutExtension
        return fileName.contains('$')
    }

    /**
     * 检查虚拟文件是否有效且存在。
     */
    private fun VirtualFile.isValidAndExists(fileContent: ByteArray? = null): Boolean =
        this.isValid && fileContent?.size != 0 && this.exists()
}

/**
 * 仓颉二进制类的接口。
 */
interface CangJieBinaryClass {
    val classId: ClassId
    val version: BinaryVersion
}

/**
 * 仓颉类头信息。
 */
data class CangJieClassHeader(
    val classId: ClassId,
    val kind: Kind,
) {
    /**
     * 仓颉类的类型。
     */
    enum class Kind {
        CLASS,
        FILE_FACADE,
        SYNTHETIC_CLASS,
        MULTIFILE_CLASS,
        MULTIFILE_CLASS_PART,
        UNKNOWN
    }
}

/**
 * 基于目录的类查找器。
 */
class DirectoryBasedClassFinder(
    private val directory: VirtualFile,
    private val packageFqName: FqName
) {
    /**
     * 查找指定的仓颉类。
     *
     * @param classId 类的标识符
     * @param version 期望的二进制版本
     * @return 找到的二进制类，如果未找到返回 null
     */
    fun findCangJieClass(classId: ClassId, version: BinaryVersion): CangJieBinaryClass? {
        val fileName = classId.relativeClassName.asString().replace('.', '$') + ".cjo"
        val file = directory.findChild(fileName) ?: return null

        if (!file.isValid || !file.exists()) {
            return null
        }

        return object : CangJieBinaryClass {
            override val classId: ClassId = classId
            override val version: BinaryVersion = version
        }
    }
}

/**
 * 仓颉二进制类缓存。
 *
 * 用于缓存文件的类型判断结果，避免重复解析。
 */
class ClsCangJieBinaryClassCache private constructor() {

    /**
     * 检查文件是否为仓颉编译文件。
     *
     * @param file 虚拟文件
     * @param fileContent 文件内容（可选）
     * @return 如果是仓颉编译文件返回 true
     */
    fun isCangJieCompiledFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        return file.extension == CangJieMetadataFileType.defaultExtension ||
                file.fileType == CangJieMetadataFileType
    }

    /**
     * 获取仓颉二进制类的头信息。
     *
     * @param file 虚拟文件
     * @param fileContent 文件内容（可选）
     * @return 头信息，如果无法解析返回 null
     */
    fun getCangJieBinaryClassHeaderData(file: VirtualFile, fileContent: ByteArray? = null): CangJieClassHeader? {
        if (!isCangJieCompiledFile(file, fileContent)) {
            return null
        }

        // 从文件名推断类型
        val fileName = file.nameWithoutExtension
        val kind = when {
            fileName.contains("$$") -> CangJieClassHeader.Kind.MULTIFILE_CLASS_PART
            fileName.contains('$') -> CangJieClassHeader.Kind.SYNTHETIC_CLASS
            else -> CangJieClassHeader.Kind.CLASS
        }

        val classId = inferClassIdFromFile(file)
        return CangJieClassHeader(classId, kind)
    }

    /**
     * 从文件路径推断类 ID。
     */
    private fun inferClassIdFromFile(file: VirtualFile): ClassId {
        val fileName = file.nameWithoutExtension
        // 简化实现：假设文件名就是类名
        return ClassId(FqName.ROOT, Name.identifier(fileName))
    }

    companion object {
        private val INSTANCE = ClsCangJieBinaryClassCache()

        fun getInstance(): ClsCangJieBinaryClassCache = INSTANCE
    }
}