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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import org.cangnova.cangjie.resolve.source.MacroExpandedSourceElement
import org.cangnova.cangjie.stubindex.CangJieMacroDeclarationShortNameIndex
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏 PSI 展开服务
 *
 * 项目级服务，负责：
 * - 管理展开结果缓存
 * - 提供逐宏展开结果查询（[getExpansionResult]）
 * - 提供文件级宏展开声明提取（[getExpandedDeclarations]）供 SyntheticResolveExtension 使用
 *
 * 缓存策略：
 * - 展开结果缓存（[expansionResultsCache]）：由后台任务预填充，解析阶段直接命中
 * - 声明缓存：使用 [CachedValuesManager]，依赖 [PsiModificationTracker] 自动失效
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
      fun isRealMacro(macroExpr: CjMacroExpression): Boolean {
        val project = macroExpr.project ?: return false
        val expansionService = getInstance(project)
        if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) {
            return true
        }
        // 通过 Stub 索引查找名字是否对应宏声明
        val shortName = macroExpr.shortName?.asString() ?: return false
        val scope = GlobalSearchScope.allScope(project)
        return CangJieMacroDeclarationShortNameIndex[shortName, project, scope].isNotEmpty()
    }
    /**
     * 单个宏展开产生的声明信息
     *
     * @param declarations 展开文本解析出的声明列表
     * @param macroExpression 原始宏表达式 PSI 节点
     * @param macroName 宏名称
     * @param sourceInfo 宏来源信息（用于标记展开元素）
     */
    data class ExpandedDeclarationInfo(
        val declarations: List<CjDeclaration>,
        val macroExpression: CjMacroExpression,
        val macroName: String,
        val sourceInfo: MacroSourceInfo
    )





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
     * 获取源文件中所有宏展开产生的声明
     *
     * 遍历文件中的每个宏表达式，获取其展开文本，
     * 将展开文本解析为临时文件提取声明，
     * 并对提取的声明附加 [MacroSourceInfo] 和 [MacroExpandedSourceElement.MACRO_EXPRESSION_KEY]。
     *
     * 结果通过 [CachedValuesManager] 缓存，源文件修改后自动失效。
     *
     * @param sourceFile 原始源文件
     * @return 展开声明信息列表，如果文件中没有宏或展开失败则返回空列表
     */
    fun getExpandedDeclarations(sourceFile: CjFile): List<ExpandedDeclarationInfo> {
        if (!isEnabled()) return emptyList()

        return CachedValuesManager.getCachedValue(sourceFile) {
            CachedValueProvider.Result.create(
                collectExpandedDeclarations(sourceFile),
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
     * 收集源文件中所有宏展开产生的声明
     *
     * 对文件中每个宏表达式：
     * 1. 从缓存获取展开结果
     * 2. 用 [CjPsiFactory] 将展开文本解析为临时 CjFile
     * 3. 提取临时文件中的声明
     * 4. 对声明附加来源信息（MacroSourceInfo + MACRO_EXPRESSION_KEY）
     */
    private fun collectExpandedDeclarations(sourceFile: CjFile): List<ExpandedDeclarationInfo> {
        val macroExprs = PsiTreeUtil.findChildrenOfType(sourceFile, CjMacroExpression::class.java)
        if (macroExprs.isEmpty()) return emptyList()

        val virtualFile = sourceFile.originalFile.virtualFile ?: return emptyList()
        val results = expansionResultsCache[virtualFile.path] ?: return emptyList()
        if (results.isEmpty()) return emptyList()

        val factory = CjPsiFactory(project)
        val infos = mutableListOf<ExpandedDeclarationInfo>()

        for (macroExpr in macroExprs) {
            // 只处理文件顶层的宏表达式，块级宏由 visitMacroExpression 处理
            if (macroExpr.parent !is CjFile) continue

            val result = matchResultToExpression(macroExpr, results) ?: continue
            if (result.expandedText.isBlank()) continue

            // 将展开文本解析为临时文件以提取声明
            val tempFile = try {
                factory.createFile("_macro_${result.macroName}.cj", result.expandedText) as? CjFile
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本失败: ${result.macroName}", e)
                null
            } ?: continue

            val declarations = tempFile.declarations.toList()
            if (declarations.isEmpty()) continue

            val macroName = result.macroName ?: macroExpr.shortName?.asString() ?: "unknown"
            val sourceInfo = MacroSourceInfo(
                originalFilePath = virtualFile.path,
                originalTextRange = macroExpr.textRange,
                macroName = macroName
            )

            // 对所有声明附加来源信息
            for (decl in declarations) {
                attachSourceInfo(decl, sourceInfo, macroExpr)
            }

            infos.add(ExpandedDeclarationInfo(declarations, macroExpr, macroName, sourceInfo))
        }

        return infos
    }

    /**
     * 将 [MacroSourceInfo] 和 [MacroExpandedSourceElement.MACRO_EXPRESSION_KEY] 附加到 PSI 元素及其所有子节点
     */
    private fun attachSourceInfo(element: PsiElement, sourceInfo: MacroSourceInfo, macroExpr: CjMacroExpression) {
        element.putUserData(MacroSourceInfo.KEY, sourceInfo)
        element.putUserData(MacroExpandedSourceElement.MACRO_EXPRESSION_KEY, macroExpr)
        var child = element.firstChild
        while (child != null) {
            attachSourceInfo(child, sourceInfo, macroExpr)
            child = child.nextSibling
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

    /**
     * 根据宏表达式所在位置和类型解析展开文本，提取解析错误和有效元素
     *
     * 位置判断：
     * - 文件顶层（`parent is CjFile`）：使用 [CjPsiFactory.createFile] 解析为顶层声明
     * - 类体内（`parent is CjAbstractClassBody`）：包装为虚拟类解析为成员声明
     * - 调用参数（`parent is CjValueArgument`）：使用 [CjPsiFactory.createExpression] 解析为表达式
     *
     * 其他情况均使用 [CjPsiFactory.createBlockCodeFragment] 解析为语句，
     * 传入 [macroExpr] 作为上下文以解析当前作用域中的符号
     *
     * @param macroExpr 宏表达式 PSI 节点（决定解析上下文）
     * @param expandedText 展开后的文本
     * @return (解析错误列表, 有效元素列表)
     */
    fun getExpansionErrorsAndElements(
        macroExpr: CjMacroExpression,
        expandedText: String
    ): Pair<List<PsiErrorElement>, List<CjElement>> {
        val factory = CjPsiFactory(macroExpr.project)
        val parent = macroExpr.parent

        if (parent is CjFile) {
            // 文件顶层宏：使用 createFile 解析为顶层声明
            val file = try {
                factory.createFile("_macro_expanded.cj", expandedText) as? CjFile
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本为文件失败", e)
                return Pair(emptyList(), emptyList())
            } ?: return Pair(emptyList(), emptyList())

            val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).toList()
            val elements = file.children.filterIsInstance<CjElement>()
            return Pair(errors, elements)
        } else if (parent is CjAbstractClassBody) {
            // 类体内宏：包装为虚拟类以正确解析成员声明（方法、属性等）
            val wrappedText = "class _MacroExpanded_ {\n$expandedText\n}"
            val file = try {
                factory.createFile("_macro_expanded.cj", wrappedText) as? CjFile
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本为类成员失败", e)
                return Pair(emptyList(), emptyList())
            } ?: return Pair(emptyList(), emptyList())

            val dummyClass = file.declarations.firstOrNull() as? CjClass
            val classBody = dummyClass?.body
            if (classBody == null) {
                val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).toList()
                return Pair(errors, emptyList())
            }

            val errors = PsiTreeUtil.findChildrenOfType(classBody, PsiErrorElement::class.java).toList()
            val elements = classBody.declarations
            return Pair(errors, elements)
        } else if (parent is CjValueArgument) {
            // 调用参数上下文：宏展开结果是一个表达式，使用 createExpression 解析
            val expr = try {
                factory.createBlockCodeFragment("call(${expandedText})",null).getContentElement()
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本为表达式失败", e)
                null
            }

            if (expr != null) {
                val errors = PsiTreeUtil.findChildrenOfType(expr, PsiErrorElement::class.java).toList()
                return Pair(errors, listOf(expr))
            }

            // 表达式解析失败，回退到代码块解析
            val block = try {
                factory.createBlockCodeFragment(expandedText, macroExpr).getContentElement()
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本为代码块失败（回退）", e)
                return Pair(emptyList(), emptyList())
            }

            val errors = PsiTreeUtil.findChildrenOfType(block, PsiErrorElement::class.java).toList()
            val elements = block.statements
            return Pair(errors, elements)
        } else {
            // 块级上下文：使用 createBlockCodeFragment 解析为语句/表达式
            // 传入 macroExpr 作为上下文，使代码片段可以解析当前作用域中的符号
            val block = try {
                factory.createBlockCodeFragment(expandedText, macroExpr).getContentElement()
            } catch (e: Exception) {
                LOG.debug("解析宏展开文本为代码块失败", e)
                return Pair(emptyList(), emptyList())
            }

            val errors = PsiTreeUtil.findChildrenOfType(block, PsiErrorElement::class.java).toList()
            val elements = block.statements
            return Pair(errors, elements)
        }
    }

    /**
     * 将注解（非宏）转换为其声明形式
     *
     * 当 CjMacroExpression 不是真正的宏（没有展开结果）时，
     * 将其 input 声明转换为带注解的声明形式。
     * 例如：@Annotation class A {} -> 将 @Annotation 转换为其声明形式
     *
     * 支持：
     * - 顶层类 (CjTypeStatement: CjStruct, CjInterface, CjEnum)
     * - 顶层/成员函数 (CjNamedFunction)
     * - 顶层/成员属性 (CjProperty)
     * - 字段变量 (CjFieldVariable)
     * - 模式变量 (CjPatternVariable)
     *
     * @param macroExpr 宏表达式（实际上是注解）
     * @return 转换后的声明元素，如果失败返回 null
     */
    fun convertAnnotationToDeclaration(
        macroExpr: CjMacroExpression
    ): CjDeclaration? {
        val inputDecl = macroExpr.input?.declarations ?: return null
        val shortName = macroExpr.shortName?.asString() ?: return null
        val attrText = macroExpr.attr?.text ?: ""

        val annotationsText = "@$shortName$attrText"
        val declarationsText = inputDecl.text

        val combinedText = """
            $annotationsText
            $declarationsText
        """.trimIndent()

        return try {
            val factory = CjPsiFactory(macroExpr.project).apply {
                isOnlyAnnotation = true
            }

            when (inputDecl) {
                is CjTypeStatement -> {
                    factory.createClass(combinedText)
                }
                is CjNamedFunction -> {
                    try {
                        factory.createFunction(combinedText)
                    } catch (e: Exception) {
                        val file = factory.createFile(combinedText)
                        file.declarations.firstOrNull()
                    }
                }
                is CjProperty -> {
                    try {
                        factory.createProperty(combinedText)
                    } catch (e: Exception) {
                        val file = factory.createFile(combinedText)
                        file.declarations.firstOrNull()
                    }
                }
                is CjFieldVariable -> {
                    try {
                        factory.createFieldVariable(combinedText)
                    } catch (e: Exception) {
                        val file = factory.createFile(combinedText)
                        file.declarations.firstOrNull()
                    }
                }
                is CjPatternVariable -> {
                    try {
                        factory.createPatternVariable(combinedText)
                    } catch (e: Exception) {
                        val file = factory.createFile(combinedText)
                        file.declarations.firstOrNull()
                    }
                }
                else -> {
                    val file = factory.createFile(combinedText)
                    file.declarations.firstOrNull()
                }
            }
        } catch (e: Exception) {
            LOG.debug("转换注解为声明失败: $shortName", e)
            null
        }
    }
}