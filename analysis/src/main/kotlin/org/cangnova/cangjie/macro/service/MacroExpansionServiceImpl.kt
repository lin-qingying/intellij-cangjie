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

package org.cangnova.cangjie.macro.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.macro.cache.MacroExpansionCache
import org.cangnova.cangjie.macro.compiler.CompilerMacroExpansionProvider
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.result.CjResult

/**
 * 宏展开服务实现
 *
 * 通过编译器 `cjc-frontend --debug-macro` 实现宏展开，并提供缓存机制。
 * 支持在展开前自动编译宏声明（通过 `cjc-frontend --compile-macro`）。
 */
@Service(Service.Level.PROJECT)
internal class MacroExpansionServiceImpl(private val project: Project) : MacroExpansionService {

    private val logger = Logger.getInstance(MacroExpansionServiceImpl::class.java)

    private val compilerProvider = CompilerMacroExpansionProvider(project)
    private val cache = MacroExpansionCache.getInstance(project)

    override suspend fun expandMacro(
        macroExpression: CjMacroExpression,
        options: MacroExpansionOptions
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        val file = macroExpression.containingFile?.virtualFile
            ?: return CjResult.Err(
                MacroExpansionError.InvalidMacroExpression("宏表达式不在有效文件中")
            )

        val offset = macroExpression.textOffset
        return expandMacroAtOffset(file, offset, options)
    }

    override suspend fun expandMacroAtOffset(
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        // 检查缓存
        if (options.useCache) {
            val cached = cache.get(file, offset)
            if (cached != null) {
                logger.debug("使用缓存的宏展开结果: ${file.path}:$offset")
                return CjResult.Ok(cached.copy(source = ExpansionSource.CACHE))
            }
        }

        // 执行宏展开
        val result = executeExpansion { provider ->
            provider.expandMacroAtOffset(project, file, offset, options)
        }

        // 缓存成功的结果
        if (result is CjResult.Ok && options.useCache) {
            cache.put(file, offset, result.ok)
        }

        return result
    }

    override suspend fun expandAllMacrosInFile(
        file: VirtualFile,
        options: MacroExpansionOptions
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        // 检查文件级缓存
        if (options.useCache) {
            val cached = cache.getForFile(file)
            if (cached != null) {
                logger.debug("使用缓存的文件宏展开结果: ${file.path}")
                return CjResult.Ok(cached.map { it.copy(source = ExpansionSource.CACHE) })
            }
        }

        // 执行宏展开
        val result = executeExpansion { provider ->
            provider.expandAllMacrosInFile(project, file, options)
        }

        // 缓存成功的结果
        if (result is CjResult.Ok && options.useCache) {
            cache.putForFile(file, result.ok)
        }

        return result
    }

    override fun isAvailable(): Boolean {
        return compilerProvider.isAvailable(project)
    }

    override fun clearCache() {
        cache.clearAll()
    }

    override fun clearCacheForFile(file: VirtualFile) {
        cache.invalidateCacheForFile(file)
    }

    /**
     * 执行宏展开操作
     */
    private suspend fun <T> executeExpansion(
        action: suspend (MacroExpansionProvider) -> CjResult<T, MacroExpansionError>
    ): CjResult<T, MacroExpansionError> {
        if (!compilerProvider.isAvailable(project)) {
            return CjResult.Err(MacroExpansionError.CompilerUnavailable("编译器不可用"))
        }
        return action(compilerProvider)
    }

    /**
     * 获取诊断信息
     */
    fun getDiagnostics(): ServiceDiagnostics {
        return ServiceDiagnostics(
            compilerAvailable = compilerProvider.isAvailable(project),
            cacheStats = cache.getStats()
        )
    }

    /**
     * 服务诊断信息
     */
    data class ServiceDiagnostics(
        val compilerAvailable: Boolean,
        val cacheStats: MacroExpansionCache.CacheStats
    )
}
