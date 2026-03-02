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
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.result.CjResult

/**
 * 宏展开服务实现
 *
 * 通过 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory] 扩展点管理宏展开引擎，支持动态注册任意数量的引擎。
 *
 * ## 引擎选择策略
 *
 * 1. 读取 [MacroExpansionSettings.preferredEngineId] 中保存的用户偏好
 * 2. 若有偏好且对应引擎可用 → 使用该引擎
 * 3. 若无偏好或偏好引擎不可用 → 使用标记了 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory.isDefault] 的引擎
 * 4. 默认引擎失败时，自动回退到其他任意可用引擎
 *
 * ## 注册新引擎
 *
 * 实现 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory] 并通过以下方式注册：
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *     <macroExpansionProviderFactory
 *         implementation="com.example.MyProviderFactory"/>
 * </extensions>
 * ```
 *
 * 注册后，用户可在设置中选择或通过 [MacroExpansionSettings] 编程切换。
 */
@Service(Service.Level.PROJECT)
internal class MacroExpansionServiceImpl(private val project: Project) : MacroExpansionService {

    private val logger = Logger.getInstance(MacroExpansionServiceImpl::class.java)

    private val cache = MacroExpansionCache.getInstance(project)

    /**
     * 已实例化的提供者缓存（按引擎 ID 索引）
     *
     * 懒创建：每个工厂首次被选中时调用 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory.createProvider]。
     * 由于服务是项目级单例，此 Map 的生命周期与项目一致。
     */
    private val providerCache = mutableMapOf<String, MacroExpansionProvider>()

    /** 所有注册的工厂（从 EP 获取） */
    private val factories: List<MacroExpansionProviderFactory>
        get() = MacroExpansionProviderFactory.EP_NAME.extensionList

    /** 获取或创建提供者实例 */
    private fun getOrCreateProvider(factory: MacroExpansionProviderFactory): MacroExpansionProvider {
        return providerCache.getOrPut(factory.engineId) { factory.createProvider(project) }
    }

    /**
     * 获取默认引擎提供者
     *
     * 查找第一个标记了 [org.cangnova.cangjie.macro.service.MacroExpansionProviderFactory.isDefault] 的工厂（按注册顺序）。
     * 若无默认工厂，则使用第一个注册的工厂。
     * 若无任何工厂注册，则抛出异常（插件配置错误）。
     */
    private fun getDefaultProvider(): MacroExpansionProvider {
        val defaultFactory = factories.firstOrNull { it.isDefault }
            ?: factories.firstOrNull()
            ?: error("未注册任何宏展开提供者工厂。请检查 macroExpansionProviderFactory 扩展点配置。")
        return getOrCreateProvider(defaultFactory)
    }

    /**
     * 根据用户持久化配置和引擎可用性选择主提供者
     *
     * 优先级：用户偏好（可用）→ 默认引擎
     */
    private fun selectProvider(): MacroExpansionProvider {
        val preferredId = MacroExpansionSettings.getInstance(project).preferredEngineId

        if (preferredId.isNotBlank()) {
            val preferredFactory = factories.firstOrNull { it.engineId == preferredId }
            if (preferredFactory != null) {
                val provider = getOrCreateProvider(preferredFactory)
                if (provider.isAvailable(project)) {
                    return provider
                }
                logger.warn("首选引擎 '$preferredId' 当前不可用，回退到默认引擎")
            } else {
                logger.warn("首选引擎 '$preferredId' 未注册（可能插件未加载），回退到默认引擎")
            }
        }

        return getDefaultProvider()
    }

    override suspend fun expandMacro(
        macroExpression: CjMacroExpression,
        options: MacroExpansionOptions,
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        val file = macroExpression.containingFile?.virtualFile
            ?: return CjResult.Err(
                MacroExpansionError.InvalidMacroExpression(CangJieMacroBundle.message("macro.error.expression.no.file"))
            )
        return expandMacroAtOffset(file, macroExpression.textOffset, options)
    }

    override suspend fun expandMacroAtOffset(
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions,
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        if (options.useCache) {
            cache.get(file, offset)?.let { cached ->
                logger.debug("缓存命中: ${file.path}:$offset")
                return CjResult.Ok(cached.copy(source = MacroExpansionSource.Cache(
                    (cached.source as? MacroExpansionSource.Engine)?.engine
                )))
            }
        }

        val result = executeWithFallback { provider ->
            provider.expandMacroAtOffset(project, file, offset, options)
        }

        if (result is CjResult.Ok && options.useCache) {
            cache.put(file, offset, result.ok)
        }
        return result
    }

    override suspend fun expandAllMacrosInFile(
        file: VirtualFile,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        if (options.useCache) {
            cache.getForFile(file)?.let { cached ->
                logger.debug("文件缓存命中: ${file.path}")
                return CjResult.Ok(cached.map { r ->
                    r.copy(source = MacroExpansionSource.Cache(
                        (r.source as? MacroExpansionSource.Engine)?.engine
                    ))
                })
            }
        }

        val result = executeWithFallback { provider ->
            provider.expandAllMacrosInFile(project, file, options)
        }

        if (result is CjResult.Ok && options.useCache) {
            cache.putForFile(file, result.ok)
        }
        return result
    }

    override fun isAvailable(): Boolean {
        return factories.any { factory ->
            getOrCreateProvider(factory).isAvailable(project)
        }
    }

    override fun clearCache() = cache.clearAll()

    override fun clearCacheForFile(file: VirtualFile) = cache.invalidateCacheForFile(file)

    /**
     * 执行展开操作，主引擎失败时自动回退到其他可用引擎
     *
     * 回退条件：主引擎返回 [MacroExpansionError.CompilerUnavailable]、
     * [MacroExpansionError.InternalError] 或 [MacroExpansionError.Timeout]。
     */
    private suspend fun <T> executeWithFallback(
        action: suspend (MacroExpansionProvider) -> CjResult<T, MacroExpansionError>,
    ): CjResult<T, MacroExpansionError> {
        val primary = selectProvider()
        if (!primary.isAvailable(project)) {
            return CjResult.Err(MacroExpansionError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.provider.unavailable", primary.name)))
        }

        val primaryResult = action(primary)

        if (primaryResult is CjResult.Err && shouldFallback(primaryResult.err)) {
            // 依次尝试其他可用引擎（排除已失败的主引擎）
            val fallbackProvider = factories
                .filter { it.engineId != primary.engine.id }
                .map { getOrCreateProvider(it) }
                .firstOrNull { it.isAvailable(project) }

            if (fallbackProvider != null) {
                logger.warn(
                    "${primary.name} 失败 (${primaryResult.err.message})，" +
                        "尝试备用引擎 ${fallbackProvider.name}"
                )
                return action(fallbackProvider)
            }
        }

        return primaryResult
    }

    /** 判断是否应该触发回退 */
    private fun shouldFallback(error: MacroExpansionError): Boolean = when (error) {
        is MacroExpansionError.CompilerUnavailable,
        is MacroExpansionError.InternalError,
        is MacroExpansionError.Timeout -> true
        else -> false
    }

    /**
     * 获取当前服务诊断信息
     */
    fun getDiagnostics(): ServiceDiagnostics {
        val allEngines = factories.map { factory ->
            val provider = getOrCreateProvider(factory)
            EngineStatus(
                engineId = factory.engineId,
                available = provider.isAvailable(project),
                isDefault = factory.isDefault,
                isActive = factory.engineId == selectProvider().engine.id,
            )
        }
        return ServiceDiagnostics(
            engines = allEngines,
            cacheStats = cache.getStats(),
        )
    }

    data class EngineStatus(
        val engineId: String,
        val available: Boolean,
        val isDefault: Boolean,
        val isActive: Boolean,
    )

    data class ServiceDiagnostics(
        val engines: List<EngineStatus>,
        val cacheStats: MacroExpansionCache.CacheStats,
    )
}
