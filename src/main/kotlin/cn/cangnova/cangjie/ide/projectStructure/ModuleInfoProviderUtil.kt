/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.ide.projectStructure

import cn.cangnova.cangjie.descriptors.ModuleInfo

import cn.cangnova.cangjie.ide.projectStructure.moduleInfo.NotUnderContentRootModuleInfo
import cn.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager


val PsiElement.moduleInfo: ModuleInfo
    get() = moduleInfoOrNull ?: NotUnderContentRootModuleInfo(project, containingFile as? CjFile)
val PsiElement.moduleInfoOrNull: ModuleInfo?
    get() {
        val anchorElement = ModuleInfoProvider.findAnchorElement(this)
        return if (anchorElement != null) {
            cachedModuleInfo(anchorElement)
        } else {
            ModuleInfoProvider.getInstance(project).firstOrNull(this)
        }
    }
fun ModuleInfoProvider.firstOrNull(virtualFile: VirtualFile): ModuleInfo? =
    collect(virtualFile).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()

fun ModuleInfoProvider.firstOrNull(
    element: PsiElement,
    config: ModuleInfoProvider.Configuration = ModuleInfoProvider.Configuration.Default
): ModuleInfo? =
    collect(element, config).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()

private fun cachedModuleInfo(
    anchorElement: PsiElement,
) = CachedValuesManager.getCachedValue(anchorElement) {
    val project = anchorElement.project
    CachedValueProvider.Result.create(
        ModuleInfoProvider.getInstance(project).firstOrNull(anchorElement),
        ProjectRootModificationTracker.getInstance(project),

//        CangJieModificationTrackerFactory.getInstance(project).createProjectWideOutOfBlockModificationTracker(),
    )
}
