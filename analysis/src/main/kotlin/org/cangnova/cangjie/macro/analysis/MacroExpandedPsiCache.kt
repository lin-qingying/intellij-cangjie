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
 */

package org.cangnova.cangjie.macro.analysis

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏展开 PSI 缓存服务
 *
 * 将宏展开的文本解析为 [CjFile] PSI 树并缓存，以便 [MacroExpandedDescriptorProvider] 提取描述符。
 *
 * 缓存策略：
 * - 缓存 key = `(filePath, startOffset)`
 * - 文件修改或删除时自动失效
 * - 跟随 [org.cangnova.cangjie.macro.cache.MacroExpansionCache] 的 TTL 策略
 */
@Service(Service.Level.PROJECT)
class MacroExpandedPsiCache(private val project: Project) : Disposable {

    private val log = Logger.getInstance(MacroExpandedPsiCache::class.java)

    private data class CacheKey(val filePath: String, val startOffset: Int)

    private data class PsiCacheEntry(
        val psiFile: CjFile,
        val timestamp: Long,
        val fileModificationStamp: Long
    )

    private val cache = ConcurrentHashMap<CacheKey, PsiCacheEntry>()

    /**
     * 文件级缓存：存储一个文件中所有宏展开的 PSI
     */
    private val filePsiCache = ConcurrentHashMap<String, List<Pair<MacroExpansionResult, CjFile>>>()
    private val filePsiTimestamps = ConcurrentHashMap<String, Long>()

    init {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    when (event) {
                        is VFileContentChangeEvent -> invalidateForFile(event.file)
                        is VFileDeleteEvent -> invalidateForFile(event.file)
                    }
                }
            }
        })
    }

    /**
     * 获取或创建宏展开结果的 PSI
     *
     * @param result 宏展开结果
     * @param sourceFile 源文件（用于获取 package 和 import 信息）
     * @return 解析后的 CjFile，若解析失败返回 null
     */
    fun getOrCreatePsi(result: MacroExpansionResult, sourceFile: VirtualFile): CjFile? {
        val key = CacheKey(result.filePath, result.startOffset)

        // 检查缓存
        val cached = cache[key]
        if (cached != null && cached.fileModificationStamp == sourceFile.modificationStamp) {
            return cached.psiFile
        }

        // 解析展开文本为 PSI
        return try {
            val psiFile = ReadAction.compute<CjFile?, Throwable> {
                createPsiFromExpandedText(result, sourceFile)
            }
            if (psiFile != null) {
                cache[key] = PsiCacheEntry(
                    psiFile = psiFile,
                    timestamp = System.currentTimeMillis(),
                    fileModificationStamp = sourceFile.modificationStamp
                )
            }
            psiFile
        } catch (e: Exception) {
            log.warn("Failed to parse macro expansion result for ${result.filePath}:${result.startOffset}", e)
            null
        }
    }

    /**
     * 批量获取或创建文件中所有宏展开的 PSI
     *
     * @param results 宏展开结果列表
     * @param sourceFile 源文件
     * @return 展开结果和对应 PSI 的列表
     */
    fun getOrCreateAllPsi(
        results: List<MacroExpansionResult>,
        sourceFile: VirtualFile
    ): List<Pair<MacroExpansionResult, CjFile>> {
        val filePath = sourceFile.path

        // 检查文件级缓存
        val cachedTimestamp = filePsiTimestamps[filePath]
        if (cachedTimestamp != null && cachedTimestamp == sourceFile.modificationStamp) {
            filePsiCache[filePath]?.let { return it }
        }

        // 逐个解析
        val parsed = results.mapNotNull { result ->
            val psi = getOrCreatePsi(result, sourceFile)
            if (psi != null) result to psi else null
        }

        // 更新文件级缓存
        filePsiCache[filePath] = parsed
        filePsiTimestamps[filePath] = sourceFile.modificationStamp

        return parsed
    }

    /**
     * 将展开后的文本解析为 CjFile PSI
     *
     * 将原文件的 package 声明和 import 列表拼接到展开文本前，使展开代码中的类型引用可以解析。
     */
    private fun createPsiFromExpandedText(result: MacroExpansionResult, sourceFile: VirtualFile): CjFile? {
        val expandedText = result.expandedText
        if (expandedText.isBlank()) return null

        // 获取原文件的 PSI 来提取 package 和 import
        val psiManager = PsiManager.getInstance(project)
        val originalPsiFile = psiManager.findFile(sourceFile) as? CjFile

        val header = buildString {
            // 拼接 package 声明
            originalPsiFile?.packageDirective?.text?.let { pkgText ->
                if (pkgText.isNotBlank()) {
                    appendLine(pkgText)
                    appendLine()
                }
            }

            // 拼接 import 声明
            originalPsiFile?.importDirectives?.forEach { importDirective ->
                appendLine(importDirective.text)
            }
            if (originalPsiFile?.importDirectives?.isNotEmpty() == true) {
                appendLine()
            }
        }

        val fullText = header + expandedText
        val fileName = "macro_expansion_${result.startOffset}.cj"

        val factory = CjPsiFactory(project)
        return factory.createFile(fileName, fullText)
    }

    /**
     * 使指定文件的缓存失效
     */
    fun invalidateForFile(file: VirtualFile) {
        val filePath = file.path
        cache.keys.removeIf { it.filePath == filePath }
        filePsiCache.remove(filePath)
        filePsiTimestamps.remove(filePath)
    }

    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cache.clear()
        filePsiCache.clear()
        filePsiTimestamps.clear()
    }

    override fun dispose() {
        clearAll()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MacroExpandedPsiCache {
            return project.getService(MacroExpandedPsiCache::class.java)
        }
    }
}
