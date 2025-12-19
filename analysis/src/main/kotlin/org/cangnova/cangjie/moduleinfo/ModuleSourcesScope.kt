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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetWithCustomData
import com.intellij.workspaceModel.core.fileIndex.impl.ModuleOrLibrarySourceRootData
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexEx.getFileInfo
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileInternalInfo
import org.cangnova.cangjie.projectStructure.scope.CombinableSourceAndClassRootsScope
import org.cangnova.cangjie.scope.AbstractVirtualFileRootsScope
import org.jetbrains.jps.model.java.JavaResourceRootType

class ModuleSourcesScope(
    private val module: Module,
    private val sourceRootKind: SourceRootKind,
) : AbstractVirtualFileRootsScope(module.project), CombinableSourceAndClassRootsScope {
    /**
     * The kind of source roots covered by the [ModuleSourcesScope].
     */
    enum class SourceRootKind {
        PRODUCTION,
        TESTS,
    }

    override val roots: Set<VirtualFile> = calculateRootsSet(module, sourceRootKind)

    /**
     * Checks if this scope is empty (has no source roots).
     */
    fun isEmpty(): Boolean = roots.isEmpty()

    override val modules: Set<Module> get() = setOf(module)

    override val includesLibraryClassRoots: Boolean get() = false

    override val includesLibrarySourceRoots: Boolean get() = false

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myProjectFileIndex.getModuleSourceOrLibraryClassesRoot(file)

    override fun isSearchInModuleContent(aModule: Module): Boolean = aModule == module

    override fun isSearchInLibraries(): Boolean = false

    override fun getDisplayName(): String = CangJieModuleInfoBundle.message("module.sources.scope.0", module.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ModuleSourcesScope) return false

        return module == other.module && sourceRootKind == other.sourceRootKind
    }

    override fun calcHashCode(): Int = sourceRootKind.hashCode() + 31 * module.hashCode()

    override fun toString(): String = "$sourceRootKind sources of module:${module.name}"

    companion object {
        fun production(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.PRODUCTION)

        fun tests(module: Module): ModuleSourcesScope =
            ModuleSourcesScope(module, SourceRootKind.TESTS)
    }
}
private fun calculateRootsSet(module: Module, sourceRootKind: ModuleSourcesScope.SourceRootKind): LinkedHashSet<VirtualFile> {
    val roots = LinkedHashSet<VirtualFile>()
    val moduleRootManager = ModuleRootManager.getInstance(module)

    for (contentEntry in moduleRootManager.contentEntries) {
        contentEntry
            .sourceFolders
            .filter { sourceFolder ->
                when {
                    sourceFolder.rootType is JavaResourceRootType -> false
                    sourceRootKind == ModuleSourcesScope.SourceRootKind.PRODUCTION -> !sourceFolder.isTestSource
                    sourceRootKind == ModuleSourcesScope.SourceRootKind.TESTS -> sourceFolder.isTestSource
                    else -> false
                }
            }
            .mapNotNullTo(roots) { it.file }
    }

    return roots
}

