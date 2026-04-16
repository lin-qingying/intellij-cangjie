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

package org.cangnova.cangjie.debugger.protobuf.memory.vfs

import com.intellij.openapi.vfs.*
import com.intellij.util.io.URLUtil
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell
import org.cangnova.cangjie.debugger.protobuf.memory.state.MemoryStore

/**
 * 内存视图虚拟文件系统
 *
 * 为内存视图提供VFS支持，使得内存区域可以像普通文件一样在IDE中打开和编辑。
 *
 * 核心特性：
 * - 虚拟文件不对应磁盘文件
 * - 内容来自调试器的内存数据
 * - 支持十六进制和反汇编两种视图
 * - 完整的IDE集成（文件历史、搜索、导航等）
 *
 * 协议格式：
 * - memory://hex
 * - memory://disasm
 */
class MemoryViewVirtualFileSystem : DeprecatedVirtualFileSystem(),
    NonPhysicalFileSystem {

    companion object {
        const val PROTOCOL = "cangjie_debugger_proto_memory"
        const val PROTOCOL_PREFIX = "$PROTOCOL${URLUtil.SCHEME_SEPARATOR}"

        /**
         * 获取单例实例
         */
        fun getInstance(): MemoryViewVirtualFileSystem {
            return VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MemoryViewVirtualFileSystem
        }
    }

    // 监听器列表
    private val listeners = mutableListOf<VirtualFileListener>()

    // 文件缓存: path -> VirtualFile
    private val fileCache = mutableMapOf<String, MemoryViewFile<*>>()

    // 线程安全锁
    private val cacheLock = Any()

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        synchronized(cacheLock) {
            return fileCache[path]
        }
    }

    override fun refresh(asynchronous: Boolean) {
        synchronized(cacheLock) {
            // 刷新所有缓存的文件
            fileCache.values.forEach { file ->
                file.refresh(false, false)
            }
        }
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        val file = findFileByPath(path)
        file?.refresh(false, false)
        return file
    }

    override fun addVirtualFileListener(listener: VirtualFileListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    override fun removeVirtualFileListener(listener: VirtualFileListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    /**
     * 获取或创建内存文件
     *
     * 使用缓存机制，避免重复创建相同的文件实例。
     * 缓存键基于 store 的 fileType 名称（每种类型只有一个文件）。
     *
     *
     * 一定要确保MemoryViewFile的path返回与这里的cacheKey一致，否则缓存机制会失效。导致各种错误
     *
     * @param store 内存存储对象
     * @return 虚拟文件
     */
    fun <T : MemoryCell> getOrCreateFile(store: MemoryStore<T>): MemoryViewFile<T> {
        val cacheKey = store.fileType.name

        synchronized(cacheLock) {
            // 尝试从缓存获取
            @Suppress("UNCHECKED_CAST")
            val cachedFile = fileCache[cacheKey] as? MemoryViewFile<T>

            if (cachedFile != null) {
                // 已存在文件，直接返回（store 在使用时会自行更新）
                return cachedFile
            }

            // 创建新文件并缓存
            val newFile = MemoryViewFile(store)
            fileCache[cacheKey] = newFile
            return newFile
        }
    }

    /**
     * 移除文件（关闭时）
     */
    fun removeFile(file: MemoryViewFile<*>) {
        synchronized(cacheLock) {
            fileCache.remove(file.path)
        }
    }

    /**
     * 获取所有打开的文件
     */
    fun getAllFiles(): List<MemoryViewFile<*>> {
        synchronized(cacheLock) {
            return fileCache.values.toList()
        }
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        synchronized(cacheLock) {
            fileCache.clear()
        }
    }

    /**
     * 获取文件数量
     */
    fun getFileCount(): Int {
        synchronized(cacheLock) {
            return fileCache.size
        }
    }

    /**
     * 根据路径模式查找文件
     *
     * @param pattern 路径模式（支持通配符）
     * @return 匹配的文件列表
     */
    fun findFilesByPattern(pattern: String): List<MemoryViewFile<*>> {
        synchronized(cacheLock) {
            val regex = pattern
                .replace("*", ".*")
                .replace("?", ".")
                .toRegex()

            return fileCache.entries
                .filter { regex.matches(it.key) }
                .map { it.value }
        }
    }

    /**
     * 检查文件是否已缓存
     */
    fun isCached(path: String): Boolean {
        synchronized(cacheLock) {
            return fileCache.containsKey(path)
        }
    }

    /**
     * 删除文件（VFS抽象方法实现）
     */
    override fun deleteFile(requestor: Any?, file: VirtualFile) {
        if (file is MemoryViewFile<*>) {
            removeFile(file)
        }
    }

    /**
     * 获取监听器列表（用于事件分发）
     */
    internal fun getListeners(): List<VirtualFileListener> {
        synchronized(listeners) {
            return listeners.toList()
        }
    }
}