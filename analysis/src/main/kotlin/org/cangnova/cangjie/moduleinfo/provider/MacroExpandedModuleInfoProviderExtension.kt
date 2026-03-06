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

package org.cangnova.cangjie.moduleinfo.provider

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.macro.expanded.MacroExpandedFileManager
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo

/**
 * 宏展开文件的模块信息提供者扩展
 *
 * 展开文件位于 `macro-expanded/` 目录下，不在项目源码根目录中，
 * 因此 [ModuleInfoProvider] 默认无法识别它们的模块归属。
 *
 * 此扩展通过 [MacroExpandedFileManager.findOriginalFile] 找到展开文件对应的原始源文件，
 * 并返回原始源文件的模块信息，使得展开文件的语义分析能够正常工作。
 */
internal class MacroExpandedModuleInfoProviderExtension : ModuleInfoProviderExtension {

    override suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByElement(
        element: PsiElement,
        file: PsiFile,
        virtualFile: VirtualFile
    ) {
        val project = element.project
        val moduleInfo = resolveModuleInfoFromExpandedFile(project, virtualFile) ?: return
        register(moduleInfo)
    }

    override suspend fun SequenceScope<Result<IdeaModuleInfo>>.collectByFile(
        project: Project,
        virtualFile: VirtualFile,
        isLibrarySource: Boolean,
        config: ModuleInfoProvider.Configuration
    ) {
        val moduleInfo = resolveModuleInfoFromExpandedFile(project, virtualFile) ?: return
        register(moduleInfo)
    }

    override suspend fun SequenceScope<Module>.findContainingModules(
        project: Project,
        virtualFile: VirtualFile
    ) {
        // 不需要额外处理，模块查找由 collectByFile 的 moduleInfo 处理
    }

    private fun resolveModuleInfoFromExpandedFile(
        project: Project,
        virtualFile: VirtualFile
    ): IdeaModuleInfo? {
        val manager = MacroExpandedFileManager.getInstance(project)
        if (!manager.isExpandedFile(virtualFile)) return null

        val originalVf = manager.findOriginalFile(virtualFile) ?: return null
        val originalPsi = PsiManager.getInstance(project).findFile(originalVf) ?: return null
        return originalPsi.moduleInfoOrNull
    }
}
