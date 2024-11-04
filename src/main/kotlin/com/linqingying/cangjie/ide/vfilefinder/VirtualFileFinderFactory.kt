package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.analyzer.MetadataFinderFactory
import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.descriptors.ModuleDescriptor

interface VirtualFileFinderFactory : MetadataFinderFactory {
    override fun create(scope: GlobalSearchScope): VirtualFileFinder
    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder

    companion object SERVICE {
        fun getInstance(project: Project): VirtualFileFinderFactory =
            project.getService(VirtualFileFinderFactory::class.java)
    }
}

class IdeVirtualFileFinderFactory : VirtualFileFinderFactory {
    override fun create(scope: GlobalSearchScope): VirtualFileFinder = IdeVirtualFileFinder(scope)

    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder {
        val ideaModuleInfo = module.getCapability(ModuleInfo.Capability)
        val scope = GlobalSearchScope.allScope(project)

//        val scope = if (ideaModuleInfo != null) {
//            CangJieResolutionScopeProvider.getInstance(project).getResolutionScope(ideaModuleInfo.toCjModule())
//        } else {
//            GlobalSearchScope.allScope(project)
//        }

        return IdeVirtualFileFinder(scope)
    }
}
