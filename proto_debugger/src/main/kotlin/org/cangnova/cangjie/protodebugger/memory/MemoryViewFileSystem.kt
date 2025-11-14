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

package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.testFramework.LightVirtualFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * 内存视图文件
 *
 * 表示内存数据的虚拟文件，用于在IDE中显示内存内容。
 *
 * @param memoryDoc 关联的内存文档
 * @param name 文件名
 * @param fileType 文件类型
 */
class MemoryViewFile(
    val memoryDoc: MemoryDoc<*>,
    name: String,
    fileType: FileType
) : LightVirtualFile(name, fileType, "") {

    override fun isWritable() = memoryDoc.dataProvider.readOnlyReason() == null

    override fun getTimeStamp() = System.currentTimeMillis()
}

/**
 * 内存视图虚拟文件系统
 *
 * 用于管理内存视图文件的虚拟文件系统。
 * 提供单例模式访问文件系统实例。
 */
object MemoryViewVirtualFileSystem : VirtualFileSystem() {
    override fun getProtocol() = "memory"

    override fun findFileByPath(path: String): VirtualFile? = null

    override fun refresh(asynchronous: Boolean) {}

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {}

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParentDir: VirtualFile) {}

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {}

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        throw UnsupportedOperationException("Not supported")
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        throw UnsupportedOperationException("Not supported")
    }

    override fun copyFile(requestor: Any?, vFile: VirtualFile, newParentDir: VirtualFile, copyName: String): VirtualFile {
        throw UnsupportedOperationException("Not supported")
    }

    override fun isReadOnly() = true


    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null

    override fun addVirtualFileListener(listener: VirtualFileListener) {}

    override fun removeVirtualFileListener(listener: VirtualFileListener) {}

    /**
     * 创建内存视图文件
     *
     * @param memoryDoc 关联的内存文档
     * @param name 文件名
     * @param fileType 文件类型
     * @return 创建的内存视图文件
     */
    fun createMemoryViewFile(
        memoryDoc: MemoryDoc<*>,
        name: String,
        fileType: FileType
    ): MemoryViewFile {
        return MemoryViewFile(memoryDoc, name, fileType)
    }

    /**
     * 获取文件系统实例
     *
     * @return 文件系统单例实例
     */
    fun getInstance(): MemoryViewVirtualFileSystem = this
}