package cn.cangnova.cangjie.ide.vfilefinder

import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.metadata.decompiler.CangJieMetadataFinder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

interface MetadataFinderFactory {
    fun create(scope: GlobalSearchScope): CangJieMetadataFinder
    fun create(project: Project, module: ModuleDescriptor): CangJieMetadataFinder
}