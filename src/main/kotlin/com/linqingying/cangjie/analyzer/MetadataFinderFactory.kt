package com.linqingying.cangjie.analyzer

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.metadata.decompiler.CangJieMetadataFinder

interface MetadataFinderFactory {
    fun create(scope: GlobalSearchScope): CangJieMetadataFinder
    fun create(project: Project, module: ModuleDescriptor): CangJieMetadataFinder
}
//interface VirtualFileFinderFactory : MetadataFinderFactory {
//    override fun create(scope: GlobalSearchScope): VirtualFileFinder
//    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder
//
//    companion object SERVICE {
//        fun getInstance(project: Project): VirtualFileFinderFactory =
//            project.getService(VirtualFileFinderFactory::class.java)
//    }
//}
