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

package org.cangnova.cangjie.macro.compiler

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile

/**
 * 全量宏包声明定位器
 *
 * 实现 [MacroDeclarationLocator]，不依赖语义分析，通过 [FileTypeIndex] 和
 * [CjFile.packageDirective] 结构性检查扫描项目中所有包含 `macro package` 声明的目录。
 *
 * ## 特点
 *
 * - **全量**：覆盖整个项目范围，不依赖 [contextFile]
 * - **无语义分析**：仅检查包声明的结构（`macro` 关键字修饰），不做引用解析
 * - **优先级低**：仅在高优先级的语义定位器无法处理时生效
 *
 * ## 适用场景
 *
 * - 触发宏展开的文件不可用，或语义定位器返回 null 时的兜底
 * - 项目首次同步、全量预编译等不依赖具体文件上下文的场景
 */
internal class FullProjectMacroDeclarationLocator : MacroDeclarationLocator {

    override val priority: Int = 0

    companion object {
        private val LOG = Logger.getInstance(FullProjectMacroDeclarationLocator::class.java)
    }

    override fun findMacroPackageDirs(project: Project, contextFile: VirtualFile?): List<String>? {
        return try {
            ReadAction.compute<List<String>, Exception> {
                findAllMacroPackageDirsImpl(project)
            }
        } catch (e: Exception) {
            LOG.warn("全量宏包定位失败", e)
            null
        }
    }

    private fun findAllMacroPackageDirsImpl(project: Project): List<String> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)

        return FileTypeIndex.getFiles(CangJieFileType.INSTANCE, scope)
            .mapNotNull { virtualFile ->
                val cjFile = psiManager.findFile(virtualFile) as? CjFile ?: return@mapNotNull null
                val packageDirective = cjFile.packageDirective ?: return@mapNotNull null
                if (packageDirective.isMacroPackage) virtualFile.parent?.path else null
            }
            .distinct()
    }
}
