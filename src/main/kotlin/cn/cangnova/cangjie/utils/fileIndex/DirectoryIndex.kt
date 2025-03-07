// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.cangnova.cangjie.utils.fileIndex

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.Query

abstract class DirectoryIndex {
    abstract fun getDirectoriesByPackageName(packageName: String, includeLibrarySources: Boolean): Query<VirtualFile>

    open fun getDirectoriesByPackageName(packageName: String, scope: GlobalSearchScope): Query<VirtualFile> {
        return getDirectoriesByPackageName(packageName, true).filtering({ file: VirtualFile ->
            scope.contains(
                file
            )
        })
    }

    abstract fun getPackageName(dir: VirtualFile): String?

    abstract fun getOrderEntries(fileOrDir: VirtualFile): List<OrderEntry>


    abstract fun getDependentUnloadedModules(module: Module): Set<String>

    companion object {

        fun getInstance(project: Project): DirectoryIndex {
            return project.service<DirectoryIndex>()

        }
    }
}
object EmptyDirectoryIndexImpl : DirectoryIndex(),
    Disposable {
    override fun getDirectoriesByPackageName(
        packageName: String,
        includeLibrarySources: Boolean
    ): Query<VirtualFile> =
        object : Query<VirtualFile> {
            override fun forEach(consumer: Processor<in VirtualFile>): Boolean {
                return false
            }

            override fun findAll(): Collection<VirtualFile> {
                return emptyList()
            }

            override fun findFirst(): VirtualFile? {
                return null
            }

        }

    override fun getPackageName(dir: VirtualFile): String? = null

    override fun getOrderEntries(fileOrDir: VirtualFile): List<OrderEntry> = emptyList()

    override fun getDependentUnloadedModules(module: Module): Set<String> = emptySet()

    override fun dispose() {
    }

    fun reset() {

    }

}
