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
package com.linqingying.cangjie.utils.roots.impl

import com.linqingying.cangjie.utils.roots.PackageIndex
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.CangJieDirectoryIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Query

internal class ProjectPackageIndexImpl(project: Project) : PackageIndex() {
    private val myDirectoryIndex: CangJieDirectoryIndex = CangJieDirectoryIndex.getInstance(project)

    override fun getDirectoriesByPackageName(packageName: String, includeLibrarySources: Boolean): Array<VirtualFile?> {
        return getDirsByPackageName(packageName, includeLibrarySources).toArray(VirtualFile.EMPTY_ARRAY)
    }

    override fun getDirsByPackageName(
        packageName: String,
        scope: GlobalSearchScope
    ): Query<VirtualFile?> {
        return myDirectoryIndex.getDirectoriesByPackageName(packageName, scope)
    }

    override fun getDirsByPackageName(packageName: String, includeLibrarySources: Boolean): Query<VirtualFile?> {
        return myDirectoryIndex.getDirectoriesByPackageName(packageName, includeLibrarySources)
    }

    override fun getPackageNameByDirectory(dir: VirtualFile): String? {
        if (!dir.isDirectory) {
            LOG.error(dir.presentableUrl + " is not a directory")
        }
        return myDirectoryIndex.getPackageName(dir)
    }

    companion object {
        private val LOG = Logger.getInstance(
            ProjectPackageIndexImpl::class.java
        )
    }
}
