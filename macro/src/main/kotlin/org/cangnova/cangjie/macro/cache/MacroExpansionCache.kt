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

package org.cangnova.cangjie.macro.cache

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 宏展开缓存服务
 *
 * 提供宏展开结果的缓存机制，支持：
 * - 基于文件和偏移量的缓存键
 * - 自动过期（5分钟）
 * - 文件修改时自动失效
 * - 手动清除
 */
@Service(Service.Level.PROJECT)
class MacroExpansionCache(private val project: Project) : Disposable {

    /**
     * 缓存条目
     */
    private data class CacheEntry(
        val result: MacroExpansionResult,
        val timestamp: Long,
        val fileModificationStamp: Long
    ) {
        /**
         * 检查缓存是否过期
         */
        fun isExpired(ttlMs: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMs
        }
    }

    /**
     * 缓存键
     */
    private data class CacheKey(
        val filePath: String,
        val offset: Int
    )

    /**
     * 单个宏的缓存
     */
    private val singleMacroCache = ConcurrentHashMap<CacheKey, CacheEntry>()

    /**
     * 文件级缓存（存储文件中所有宏的展开结果）
     */
    private val fileMacroCache = ConcurrentHashMap<String, FileCacheEntry>()

    /**
     * 文件缓存条目
     */
    private data class FileCacheEntry(
        val results: List<MacroExpansionResult>,
        val timestamp: Long,
        val fileModificationStamp: Long
    ) {
        fun isExpired(ttlMs: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMs
        }
    }

    /**
     * 缓存过期时间（毫秒）
     */
    private val cacheTtlMs: Long = TimeUnit.MINUTES.toMillis(5)

    init {
        // 监听文件变化，自动清除相关缓存
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    when (event) {
                        is VFileContentChangeEvent -> {
                            invalidateCacheForFile(event.file)
                        }
                        is VFileDeleteEvent -> {
                            invalidateCacheForFile(event.file)
                        }
                    }
                }
            }
        })
    }

    /**
     * 获取单个宏的缓存结果
     *
     * @param file 源文件
     * @param offset 偏移量
     * @return 缓存的结果，如果不存在或已过期则返回 null
     */
    fun get(file: VirtualFile, offset: Int): MacroExpansionResult? {
        val key = CacheKey(file.path, offset)
        val entry = singleMacroCache[key] ?: return null

        // 检查是否过期
        if (entry.isExpired(cacheTtlMs)) {
            singleMacroCache.remove(key)
            return null
        }

        // 检查文件是否已修改
        if (entry.fileModificationStamp != file.modificationStamp) {
            singleMacroCache.remove(key)
            return null
        }

        return entry.result
    }

    /**
     * 缓存单个宏的展开结果
     *
     * @param file 源文件
     * @param offset 偏移量
     * @param result 展开结果
     */
    fun put(file: VirtualFile, offset: Int, result: MacroExpansionResult) {
        val key = CacheKey(file.path, offset)
        val entry = CacheEntry(
            result = result,
            timestamp = System.currentTimeMillis(),
            fileModificationStamp = file.modificationStamp
        )
        singleMacroCache[key] = entry
    }

    /**
     * 获取文件级缓存结果
     *
     * @param file 源文件
     * @return 缓存的结果列表，如果不存在或已过期则返回 null
     */
    fun getForFile(file: VirtualFile): List<MacroExpansionResult>? {
        val entry = fileMacroCache[file.path] ?: return null

        // 检查是否过期
        if (entry.isExpired(cacheTtlMs)) {
            fileMacroCache.remove(file.path)
            return null
        }

        // 检查文件是否已修改
        if (entry.fileModificationStamp != file.modificationStamp) {
            fileMacroCache.remove(file.path)
            return null
        }

        return entry.results
    }

    /**
     * 缓存文件中所有宏的展开结果
     *
     * @param file 源文件
     * @param results 展开结果列表
     */
    fun putForFile(file: VirtualFile, results: List<MacroExpansionResult>) {
        val entry = FileCacheEntry(
            results = results,
            timestamp = System.currentTimeMillis(),
            fileModificationStamp = file.modificationStamp
        )
        fileMacroCache[file.path] = entry

        // 同时更新单个宏缓存
        for (result in results) {
            val key = CacheKey(file.path, result.startOffset)
            val singleEntry = CacheEntry(
                result = result,
                timestamp = System.currentTimeMillis(),
                fileModificationStamp = file.modificationStamp
            )
            singleMacroCache[key] = singleEntry
        }
    }

    /**
     * 使指定文件的缓存失效
     *
     * @param file 文件
     */
    fun invalidateCacheForFile(file: VirtualFile) {
        val filePath = file.path
        fileMacroCache.remove(filePath)

        // 移除该文件相关的单个宏缓存
        singleMacroCache.keys.removeIf { it.filePath == filePath }
    }

    /**
     * 清除所有缓存
     */
    fun clearAll() {
        singleMacroCache.clear()
        fileMacroCache.clear()
    }

    /**
     * 清除过期的缓存条目
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()

        singleMacroCache.entries.removeIf { (_, entry) ->
            entry.isExpired(cacheTtlMs)
        }

        fileMacroCache.entries.removeIf { (_, entry) ->
            entry.isExpired(cacheTtlMs)
        }
    }

    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats {
        return CacheStats(
            singleMacroCacheSize = singleMacroCache.size,
            fileCacheSize = fileMacroCache.size
        )
    }

    override fun dispose() {
        clearAll()
    }

    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val singleMacroCacheSize: Int,
        val fileCacheSize: Int
    )

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目
         * @return 缓存服务实例
         */
        @JvmStatic
        fun getInstance(project: Project): MacroExpansionCache {
            return project.getService(MacroExpansionCache::class.java)
        }
    }
}
