/*
 * Copyright 2025 LinQingYing. and contributors.
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

@file:JvmName("ModuleInfoProviderUtils")
package org.cangnova.cangjie.moduleinfo.provider

import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider.Result.create
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.moduleinfo.BinaryModuleInfo
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.LibrarySourceInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.NotUnderContentRootModuleInfo
import org.cangnova.cangjie.psi.CjFile
import kotlin.sequences.firstOrNull


val PsiElement.moduleInfo: IdeaModuleInfo
    get() = moduleInfoOrNull ?: NotUnderContentRootModuleInfo(project, containingFile as? CjFile)


val PsiElement.moduleInfoOrNull: IdeaModuleInfo?
    get() = cachedModuleInfo(this)

private fun cachedModuleInfo(
    element: PsiElement,
): IdeaModuleInfo? = CachedValuesManager.getCachedValue<IdeaModuleInfo?>(element) {
    val project = element.project
    create(
        ModuleInfoProvider.getInstance(project).firstOrNull(element),
        ProjectRootModificationTracker.getInstance(project),
    )
}


fun ModuleInfoProvider.firstOrNull(element: PsiElement, config: ModuleInfoProvider.Configuration = ModuleInfoProvider.Configuration.Default): IdeaModuleInfo? =
    collect(element, config).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()


fun ModuleInfoProvider.firstOrNull(virtualFile: VirtualFile): IdeaModuleInfo? =
    collect(virtualFile).unwrap(ModuleInfoProvider.LOG::warn).firstOrNull()


fun ModuleInfoProvider.collectLibraryBinariesModuleInfos(virtualFile: VirtualFile): Sequence<BinaryModuleInfo> {
    return collectOfType<BinaryModuleInfo>(virtualFile)
}


fun ModuleInfoProvider.collectLibrarySourcesModuleInfos(virtualFile: VirtualFile): Sequence<LibrarySourceInfo> {
    return collectOfType<LibrarySourceInfo>(virtualFile)
}


fun ModuleInfo.unwrapModuleSourceInfo(): ModuleSourceInfo? {
    return this as? ModuleSourceInfo
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
private inline fun <reified T : IdeaModuleInfo> ModuleInfoProvider.collectOfType(file: VirtualFile): Sequence<@kotlin.internal.NoInfer T> =
    collect(file, isLibrarySource = false).unwrap(ModuleInfoProvider.LOG::warn).filterIsInstance<T>()