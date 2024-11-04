package com.linqingying.cangjie.ide.projectStructure.scope

import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
interface TopPackageNamesProvider {
    val topPackageNames: Set<String>?
}
internal open  class PoweredLibraryScopeBase (project: Project, classes: Array<VirtualFile>, sources: Array<VirtualFile>) :
    LibraryScopeBase(project, classes, sources), TopPackageNamesProvider {
    override val topPackageNames: Set<String> by lazy {
        (classes + sources)
            .flatMap { it.children.toList() }
            .filter(VirtualFile::isDirectory)
            .map(VirtualFile::getName)
            .toSet() + "" // empty package is always present
    }
}


