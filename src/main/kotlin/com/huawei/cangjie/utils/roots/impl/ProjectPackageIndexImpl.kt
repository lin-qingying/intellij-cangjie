// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.huawei.cangjie.utils.roots.impl

import com.huawei.cangjie.utils.roots.PackageIndex
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
