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

package org.cangnova.cangjie.macro.expanded

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.CjProjectsService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏展开文件管理器
 *
 * 管理编译器生成的 `.macrocall` 文件处理后的展开文件：
 * - 处理 `.macrocall` 文件，生成干净的 `.cj` 展开文件和偏移映射
 * - 展开文件输出到构建输出目录下的 `macro-expanded/` 子目录
 * - 维护偏移映射的内存缓存
 * - 提供 VirtualFile 级别的展开文件访问
 *
 * ## 目录结构
 *
 * 展开文件位于构建输出目录（来自 `CjSourceSet.outputDirectory`）下：
 *
 * ```
 * <outputDirectory>/
 * ├── release/<platform>/      (编译产物)
 * └── macro-expanded/          (宏展开文件)
 *     └── src/
 *         └── main.cj          (干净的展开文件)
 * ```
 *
 * 回退策略（与 `CompilerMacroCompilationProvider` 一致）：
 * 1. 使用 `CjSourceSet.outputDirectory`
 * 2. 项目有效但无输出目录时，使用 `<projectRoot>/target/macro-expanded/`
 * 3. 无项目信息时，使用 `<projectBasePath>/target/macro-expanded/`
 */
@Service(Service.Level.PROJECT)
class MacroExpandedFileManager(private val project: Project) : Disposable {

    private val log = Logger.getInstance(MacroExpandedFileManager::class.java)

    /** 偏移映射缓存：key = 原始源文件路径 */
    private val offsetMappings = ConcurrentHashMap<String, MacroExpansionOffsetMapping>()

    /** 展开文件路径缓存：key = 原始源文件路径, value = 展开文件路径 */
    private val expandedFilePaths = ConcurrentHashMap<String, String>()

    companion object {
        private const val MACRO_EXPANDED_DIR = "macro-expanded"

        @JvmStatic
        fun getInstance(project: Project): MacroExpandedFileManager {
            return project.getService(MacroExpandedFileManager::class.java)
        }
    }

    /**
     * 处理编译器输出的 `.macrocall` 文件
     *
     * 读取 `.macrocall` 文件内容，剥离标记注释，生成干净的展开 `.cj` 文件，
     * 并构建偏移映射。
     *
     * @param sourceFile 原始源文件
     * @param macroCallContent `.macrocall` 文件的完整内容（已读取）
     * @return 处理后的展开文件 VirtualFile，如果处理失败或无宏标记则返回 null
     */
    fun processAndWrite(sourceFile: VirtualFile, macroCallContent: String): VirtualFile? {
        val sourceFilePath = sourceFile.path
        val expandedFilePath = resolveExpandedFilePath(sourceFile)
            ?: return null

        val result = MacroExpandedFileProcessor.process(
            macroCallContent = macroCallContent,
            sourceFilePath = sourceFilePath,
            expandedFilePath = expandedFilePath
        ) ?: return null

        return try {
            val expandedFile = File(expandedFilePath)
            expandedFile.parentFile?.mkdirs()
            expandedFile.writeText(result.cleanContent, Charsets.UTF_8)

            // 缓存偏移映射
            offsetMappings[sourceFilePath] = result.offsetMapping
            expandedFilePaths[sourceFilePath] = expandedFilePath

            // 持久化偏移映射到 JSON 文件
            MacroExpansionOffsetMapping.saveToDisk(result.offsetMapping)

            // 刷新 VFS 使 IntelliJ 感知新文件
            LocalFileSystem.getInstance().refreshAndFindFileByPath(expandedFilePath)
        } catch (e: Exception) {
            log.warn("写入宏展开文件失败: $expandedFilePath", e)
            null
        }
    }

    /**
     * 获取原始源文件对应的偏移映射
     *
     * 优先从内存缓存读取，未命中时尝试从磁盘 JSON 文件加载并回填缓存。
     */
    fun getOffsetMapping(sourceFilePath: String): MacroExpansionOffsetMapping? {
        offsetMappings[sourceFilePath]?.let { return it }

        // 内存未命中，尝试从磁盘加载
        val expandedPath = expandedFilePaths[sourceFilePath]
            ?: resolveExpandedFilePathBySourcePath(sourceFilePath)
            ?: return null

        val loaded = MacroExpansionOffsetMapping.loadFromDisk(expandedPath) ?: return null
        offsetMappings[sourceFilePath] = loaded
        expandedFilePaths[sourceFilePath] = expandedPath
        return loaded
    }

    /**
     * 获取原始源文件对应的展开文件
     */
    fun getExpandedFile(sourceFile: VirtualFile): VirtualFile? {
        val expandedPath = expandedFilePaths[sourceFile.path]
            ?: resolveExpandedFilePath(sourceFile)
            ?: return null
        return LocalFileSystem.getInstance().findFileByPath(expandedPath)
    }

    /**
     * 检查给定的 VirtualFile 是否是宏展开生成的文件
     */
    fun isExpandedFile(file: VirtualFile): Boolean {
        val expandedDir = getExpandedDirectory() ?: return false
        val expandedDirPath = expandedDir.absolutePath.replace('\\', '/')
        return file.path.startsWith(expandedDirPath)
    }

    /**
     * 根据展开文件查找对应的原始源文件
     */
    fun findOriginalFile(expandedFile: VirtualFile): VirtualFile? {
        val expandedDir = getExpandedDirectory() ?: return null
        val expandedDirPath = expandedDir.absolutePath.replace('\\', '/')
        val expandedPath = expandedFile.path

        if (!expandedPath.startsWith(expandedDirPath)) return null

        // 从展开路径还原原始源文件路径：相对路径对应源码根目录下的位置
        val relativePath = expandedPath.removePrefix(expandedDirPath).removePrefix("/")
        val projectBasePath = project.basePath ?: return null
        val originalPath = "$projectBasePath/$relativePath"

        return LocalFileSystem.getInstance().findFileByPath(originalPath)
    }

    /**
     * 使指定源文件的展开缓存失效
     */
    fun invalidate(sourceFilePath: String) {
        offsetMappings.remove(sourceFilePath)
        val expandedPath = expandedFilePaths.remove(sourceFilePath) ?: return
        try {
            MacroExpansionOffsetMapping.deleteFromDisk(expandedPath)
            File(expandedPath).delete()
        } catch (e: Exception) {
            log.debug("删除展开文件失败: $expandedPath", e)
        }
    }

    /**
     * 清除所有展开文件和缓存
     */
    fun clearAll() {
        offsetMappings.clear()
        expandedFilePaths.clear()

        val expandedDir = getExpandedDirectory()
        if (expandedDir != null && expandedDir.exists()) {
            try {
                expandedDir.deleteRecursively()
            } catch (e: Exception) {
                log.warn("清除宏展开目录失败: ${expandedDir.absolutePath}", e)
            }
        }
    }

    /**
     * 获取展开文件输出目录
     *
     * 优先使用 cangjie-project 提供的构建输出目录，回退逻辑与
     * [org.cangnova.cangjie.macro.compiler.CompilerMacroCompilationProvider] 一致。
     *
     * @return `<outputDirectory>/macro-expanded/` 目录
     */
    fun getExpandedDirectory(): File? {
        val projectsService = try {
            CjProjectsService.getInstance(project)
        } catch (_: Exception) {
            return fallbackExpandedDirectory()
        }

        val cjProject = projectsService.cjProject

        // 收集所有模块的输出目录
        val allModules = buildList {
            cjProject.module?.let { add(it) }
            cjProject.workspace?.modules?.let { addAll(it) }
        }
        val outputDirs = allModules
            .flatMap { it.sourceSets }
            .flatMap { it.outputDirectory }

        return if (outputDirs.isNotEmpty()) {
            File(outputDirs.first().path, MACRO_EXPANDED_DIR)
        } else if (cjProject.isValid) {
            File(cjProject.rootDir.path, "target/$MACRO_EXPANDED_DIR")
        } else {
            fallbackExpandedDirectory()
        }
    }

    /**
     * 回退：使用项目基础路径
     */
    private fun fallbackExpandedDirectory(): File? {
        val basePath = project.basePath ?: return null
        return File(basePath, "target/$MACRO_EXPANDED_DIR")
    }

    /**
     * 计算源文件对应的展开文件路径
     *
     * 保持源文件相对于项目根目录的路径结构，将其映射到展开目录中。
     */
    private fun resolveExpandedFilePath(sourceFile: VirtualFile): String? {
        return resolveExpandedFilePathBySourcePath(sourceFile.path.replace('\\', '/'))
    }

    /**
     * 通过源文件路径字符串计算展开文件路径
     *
     * 用于不持有 VirtualFile 引用的场景（如从磁盘恢复映射时）。
     */
    private fun resolveExpandedFilePathBySourcePath(sourceFilePath: String): String? {
        val basePath = project.basePath ?: return null
        val normalizedBasePath = basePath.replace('\\', '/')
        val normalizedSourcePath = sourceFilePath.replace('\\', '/')

        val relativePath = if (normalizedSourcePath.startsWith(normalizedBasePath)) {
            normalizedSourcePath.removePrefix(normalizedBasePath).removePrefix("/")
        } else {
            normalizedSourcePath.substringAfterLast('/')
        }

        val expandedDir = getExpandedDirectory() ?: return null
        return File(expandedDir, relativePath).absolutePath.replace('\\', '/')
    }

    override fun dispose() {
        clearAll()
    }
}