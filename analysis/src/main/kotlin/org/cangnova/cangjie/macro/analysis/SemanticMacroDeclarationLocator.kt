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

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.macro.compiler.MacroDeclarationLocator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression

/**
 * 基于语义分析的宏声明定位器
 *
 * 实现 [MacroDeclarationLocator]，通过 PSI 引用解析精确定位 [contextFile] 中
 * 每个宏调用表达式（[CjMacroExpression]）所指向的宏声明所在目录。
 *
 * ## 工作原理
 *
 * 1. 从 [contextFile] 的 PSI 中收集所有 [CjMacroExpression]
 * 2. 通过 `referenceExpression.reference.resolve()` 获取每个宏调用的声明指向源
 * 3. 取声明所在文件的父目录作为需要编译的宏包目录
 *
 * 相比全量扫描，只编译 [contextFile] 实际使用的宏包，效率更高。
 *
 * ## 注意
 *
 * - [contextFile] 为 null 时返回 null，由框架回退到 [FullProjectMacroDeclarationLocator]
 * - 引用解析失败时返回 null，同样触发回退
 */
internal class SemanticMacroDeclarationLocator : MacroDeclarationLocator {

    override val priority: Int = 10

    companion object {
        private val LOG = Logger.getInstance(SemanticMacroDeclarationLocator::class.java)
    }

    override fun findMacroPackageDirs(project: Project, contextFile: VirtualFile?): List<String>? {
        if (contextFile == null) return null

        return try {
            ReadAction.compute<List<String>?, Exception> {
                findMacroPackageDirsForFile(project, contextFile)
            }
        } catch (e: Exception) {
            LOG.warn("语义宏声明定位失败，回退到全量定位器", e)
            null
        }
    }

    private fun findMacroPackageDirsForFile(project: Project, contextFile: VirtualFile): List<String>? {
        val psiManager = PsiManager.getInstance(project)
        val cjFile = psiManager.findFile(contextFile) as? CjFile ?: return null

        val macroDirs = PsiTreeUtil.findChildrenOfType(cjFile, CjMacroExpression::class.java)
            .mapNotNull { macroExpr ->
                // 通过引用解析获取宏声明的指向源
                macroExpr.referenceExpression
                    ?.reference
                    ?.resolve()
                    ?.containingFile
                    ?.virtualFile
                    ?.parent
                    ?.path
            }
            .distinct()

        // 解析到了声明但结果为空（文件中有宏调用但引用均解析失败），回退到全量定位器
        return macroDirs.ifEmpty { null }
    }
}
