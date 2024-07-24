package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.AbstractResolverForProject
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.analyzer.ResolverForModule
import com.huawei.cangjie.analyzer.ResolverForProject
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.context.ProjectContext
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.idea.projectStructure.moduleInfo.IdeaModuleInfo
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.util.ModificationTracker

class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    modules: Collection<IdeaModuleInfo>,
    private val syntheticFilesByModule: Map<IdeaModuleInfo, Collection<CjFile>>,
    delegateResolver: ResolverForProject<IdeaModuleInfo>,
    fallbackModificationTracker: ModificationTracker? = null,

    ) : AbstractResolverForProject<IdeaModuleInfo>(

    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
) {
    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: IdeaModuleInfo): ResolverForModule {
        TODO("Not yet implemented")
    }

    override fun builtInsForModule(module: IdeaModuleInfo): CangJieBuiltIns {
        TODO("Not yet implemented")
    }




}
