package com.linqingying.cangjie.resolve.caches

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.resolve.scopes.optimization.OptimizingOptions


interface ResolveOptimizingOptionsProvider {
    fun getOptimizingOptions(project: Project, descriptor: ModuleDescriptor, moduleInfo: ModuleInfo): OptimizingOptions?

    companion object {
        val EP_NAME = ExtensionPointName.create<ResolveOptimizingOptionsProvider>("com.linqingying.cangjie.ide.caches.resolve.resolveOptimizingOptionsProvider")

        fun getOptimizingOptions(project: Project, descriptor: ModuleDescriptor, moduleInfo: ModuleInfo): OptimizingOptions? {
            return EP_NAME.extensions.firstNotNullOfOrNull { extension ->
                extension.getOptimizingOptions(project, descriptor, moduleInfo)
            }
        }
    }
}
