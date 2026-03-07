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

package org.cangnova.cangjie.macro.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏 PSI 展开服务
 *
 * 项目级服务，负责：
 * - 管理展开结果缓存和展开后的 PSI 文件副本缓存
 * - 协调宏展开和 PSI 替换
 * - 提供 [getExpandedFile] 接口供 SyntheticResolveExtension 使用
 *
 * 缓存策略：
 * - 展开结果缓存（[expansionResultsCache]）：由后台任务预填充，解析阶段直接命中
 * - PSI 副本缓存：使用 [CachedValuesManager]，依赖 [PsiModificationTracker] 自动失效
 */
@Service(Service.Level.PROJECT)
class MacroPsiExpansionService(private val project: Project) {

    companion object {
        private val LOG = Logger.getInstance(MacroPsiExpansionService::class.java)

        @JvmStatic
        fun getInstance(project: Project): MacroPsiExpansionService {
            return project.getService(MacroPsiExpansionService::class.java)
        }
    }

    /**
     * 展开结果缓存：filePath → 展开结果列表
     *
     * 由 [MacroExpansionBackgroundTask] 在后台预填充，
     * [expandFileInternal] 优先从此缓存获取结果，
     * 避免在解析阶段依赖宏展开引擎实时可用。
     */
    private val expansionResultsCache = ConcurrentHashMap<String, List<MacroExpansionResult>>()

    /**
     * 获取源文件的宏展开 PSI 副本
     *
     * 如果文件中包含宏调用且展开成功，返回替换了宏调用的 PSI 副本。
     * 结果通过 [CachedValuesManager] 缓存，源文件修改后自动失效。
     *
     * @param sourceFile 原始源文件
     * @return 展开后的 PSI 副本，如果文件中没有宏或展开失败则返回 null
     */
    fun getExpandedFile(sourceFile: CjFile): CjFile? {
        if (!isEnabled()) return null

        return CachedValuesManager.getCachedValue(sourceFile) {
            val expandedFile = expandFileInternal(sourceFile)
            CachedValueProvider.Result.create(
                expandedFile,
                PsiModificationTracker.getInstance(project)
            )
        }
    }

    /**
     * 缓存文件的展开结果
     *
     * 由后台任务调用，预填充展开结果缓存。
     */
    fun cacheExpansionResults(filePath: String, results: List<MacroExpansionResult>) {
        expansionResultsCache[filePath] = results
    }

    /**
     * 清除所有展开缓存
     *
     * 由管线协调器在宏重新编译后调用。
     */
    fun clearCache() {
        expansionResultsCache.clear()
        LOG.debug("宏 PSI 展开缓存已清除")
    }

    /**
     * 检查宏展开分析是否启用
     */
    fun isEnabled(): Boolean {
        return try {
            Registry.`is`("cangjie.macro.expansion.analysis.enabled", false)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 执行文件的宏展开和 PSI 替换
     *
     * 仅从本地展开结果缓存获取结果（由后台任务预填充），
     * 解析阶段不做阻塞的展开调用，避免死锁。
     */
    private fun expandFileInternal(sourceFile: CjFile): CjFile? {
        // 快速检查：文件中是否有宏调用
        val macroExprs = PsiTreeUtil.findChildrenOfType(sourceFile, CjMacroExpression::class.java)
        if (macroExprs.isEmpty()) return null

        val virtualFile = sourceFile.virtualFile ?: return null

        // 仅使用本地缓存，不做阻塞展开
        val results = expansionResultsCache[virtualFile.path] ?: return null
        if (results.isEmpty()) return null

        // 构建替换列表：将展开结果匹配到 PSI 宏表达式
        val replacements = matchResultsToExpressions(macroExprs, results)
        if (replacements.isEmpty()) return null

        // 执行 PSI 替换
        return MacroPsiReplacer.replaceInCopy(sourceFile, replacements)
    }

    /**
     * 将展开结果匹配到 PSI 宏表达式
     */
    private fun matchResultsToExpressions(
        macroExprs: Collection<CjMacroExpression>,
        results: List<MacroExpansionResult>
    ): List<MacroPsiReplacer.MacroReplacement> {
        val replacements = mutableListOf<MacroPsiReplacer.MacroReplacement>()

        for (macroExpr in macroExprs) {
            val exprOffset = macroExpr.textOffset

            // 查找匹配的展开结果（通过偏移量范围重叠匹配）
            val matchingResult = results.find { result ->
                result.startOffset != result.endOffset &&
                    exprOffset >= result.startOffset && exprOffset < result.endOffset
            } ?: results.find { result ->
                // 回退：使用宏名称匹配
                result.macroName != null && result.macroName == macroExpr.shortName?.asString()
            }

            if (matchingResult != null && matchingResult.expandedText.isNotBlank()) {
                replacements.add(
                    MacroPsiReplacer.MacroReplacement(
                        macroExpression = macroExpr,
                        expandedText = matchingResult.expandedText,
                        macroName = matchingResult.macroName ?: macroExpr.shortName?.asString() ?: "unknown"
                    )
                )
            }
        }

        return replacements
    }
}
