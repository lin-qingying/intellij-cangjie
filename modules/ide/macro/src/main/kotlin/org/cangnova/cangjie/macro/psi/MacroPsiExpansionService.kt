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
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏 PSI 展开服务
 *
 * 项目级服务，负责：
 * - 管理展开结果缓存
 * - 提供逐宏展开结果查询（[getExpansionResult]）
 * - 提供 IDE gutter / preview / debug 所需的展开文本缓存
 *
 * 缓存策略：
 * - 展开结果缓存（[expansionResultsCache]）：由后台任务预填充，仅供 gutter、preview、debug 查询
 *
 * 注意：该服务不得把 `expandedText` 解析为临时 PSI 并交给 resolve / analysis /
 * synthetic declaration provider。语义路径必须走 CFIR macro construction。
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
     * 由 [MacroExpansionBackgroundTask][org.cangnova.cangjie.macro.analysis.MacroExpansionBackgroundTask] 在后台预填充，
     * 解析阶段直接从此缓存获取结果，避免阻塞等待宏展开引擎。
     */
    private val expansionResultsCache = ConcurrentHashMap<String, List<MacroExpansionResult>>()

    /**
     * 获取单个宏表达式的展开结果
     *
     * 从缓存中查找与给定宏表达式匹配的展开结果。
     * 通过偏移量范围匹配，回退使用宏名称匹配。
     *
     * @param macroExpr 宏表达式 PSI 节点
     * @return 匹配的展开结果，如果缓存未命中则返回 null
     */
    fun getExpansionResult(macroExpr: CjMacroExpression): MacroExpansionResult? {
        if (!isEnabled()) return null
        val file = macroExpr.containingFile?.virtualFile ?: return null
        val results = expansionResultsCache[file.path] ?: return null
        return matchResultToExpression(macroExpr, results)
    }

    /**
     * 缓存文件的展开结果。
     *
     * 由后台任务调用，预填充 IDE 展示/调试缓存；不得作为语义分析输入。
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
     * 将展开结果匹配到特定宏表达式
     *
     * 优先通过偏移量范围匹配，回退使用宏名称匹配。
     *
     * 注意：对于注解（非宏），由于没有展开结果，此方法会返回 null。
     * 这是预期行为，因为注解不会被展开。
     */
    private fun matchResultToExpression(
        macroExpr: CjMacroExpression,
        results: List<MacroExpansionResult>
    ): MacroExpansionResult? {
        val exprOffset = macroExpr.textOffset
        val exprName = macroExpr.shortName?.asString()

        // 优先：通过偏移量范围精确匹配
        val offsetMatch = results.find { result ->
            result.startOffset != result.endOffset &&
                    exprOffset >= result.startOffset && exprOffset < result.endOffset
        }
        if (offsetMatch != null) {
            return offsetMatch
        }

        // 回退：使用宏名称匹配
        // 注意：这里需要谨慎处理，因为注解和宏可能有相同的名称
        // 只有当名称完全匹配时才返回结果
        if (exprName != null) {
            val nameMatch = results.find { result ->
                result.macroName == exprName
            }
            if (nameMatch != null) {
                // 进一步验证：确保名称匹配的结果的偏移量在合理范围内
                // （不应该匹配到距离太远的结果）
                val distance = kotlin.math.abs(nameMatch.startOffset - exprOffset)
                if (distance < 100) { // 100 字符以内的容差
                    return nameMatch
                }
            }
        }

        return null
    }
}
