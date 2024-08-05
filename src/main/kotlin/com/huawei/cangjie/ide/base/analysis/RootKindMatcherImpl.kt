package com.huawei.cangjie.ide.base.analysis

import com.huawei.cangjie.ide.base.projectStructure.RootKindFilter
import com.huawei.cangjie.ide.base.projectStructure.RootKindMatcher
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

internal class RootKindMatcherImpl(private val project: Project) : RootKindMatcher {
    private val fileIndex by lazy { ProjectRootManager.getInstance(project).fileIndex }


    override fun matches(filter: RootKindFilter, virtualFile: VirtualFile): Boolean {
        ProgressManager.checkCanceled()

//        val rootType = if (filter.includeResources) {
//            CANGJIE_AWARE_SOURCE_AND_RESOURCES_ROOT_TYPES
//        } else {
//            CANGJIE_AWARE_SOURCE_ROOT_TYPES
//        }

        if (virtualFile !is VirtualFileWindow && fileIndex.isInSource(virtualFile)) {
            return filter.includeProjectSourceFiles
        }

        return false


    }
}
