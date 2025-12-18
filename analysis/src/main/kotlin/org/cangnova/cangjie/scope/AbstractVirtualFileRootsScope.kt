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
package org.cangnova.cangjie.scope

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import com.intellij.psi.search.impl.VirtualFileEnumerationAware
import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.jetbrains.annotations.ApiStatus

/**
 * A base implementation of scopes based on a single virtual file roots map, such as [ModuleSourcesScope].
 *
 * While [ModuleSourcesScope] is currently the only implementation, this class can also be used as a base for
 * `ModulesWithDependenciesScope` once [ModuleSourcesScope] is migrated to the platform.
 */

abstract class AbstractVirtualFileRootsScope(project: Project) : GlobalSearchScope(project)  {

    @Volatile
    private var vfsModificationCount: Long = 0


    /**
     * A map from [VirtualFile] roots to an integer which represents the position of the root in the classpath.
     */
    protected abstract val roots: Object2IntMap<VirtualFile>

    protected abstract fun getFileRoot(file: VirtualFile): VirtualFile?

    override fun contains(file: VirtualFile): Boolean {
        // Note: The scope's logic is copied from `ModuleWithDependenciesScope`, which has an additional check for Bazel single-file
        // modules. The check in `ModuleWithDependenciesScope` is actually a remnant of a failed experiment, so omitting the check here is
        // fine.
        val root = getFileRoot(file) ?: return false
        return roots.containsKey(root)
    }

    override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
        val r1 = getFileRoot(file1)
        val r2 = getFileRoot(file2)
        if (Comparing.equal(r1, r2)) return 0

        if (r1 == null) return -1
        if (r2 == null) return 1

        val roots = roots
        val i1 = roots.getInt(r1)
        val i2 = roots.getInt(r2)
        if (i1 == 0 && i2 == 0) return 0
        if (i1 > 0 && i2 > 0) return i2 - i1
        return if (i1 > 0) 1 else -1
    }


}
