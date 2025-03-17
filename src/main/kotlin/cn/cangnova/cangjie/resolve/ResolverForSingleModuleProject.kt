package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.config.LanguageVersionSettingsImpl
import cn.cangnova.cangjie.context.ProjectContext
import cn.cangnova.cangjie.context.withModule
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.caches.ModuleContent
import cn.cangnova.cangjie.types.DefaultBuiltIns
import com.intellij.psi.search.GlobalSearchScope

class ResolverForSingleModuleProject<M : ModuleInfo>(
    debugName: String,
    projectContext: ProjectContext,
    private val module: M,
    private val resolverForModuleFactory: ResolverForModuleFactory,
    private val searchScope: GlobalSearchScope,
    private val builtIns: CangJieBuiltIns = DefaultBuiltIns,
    private val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.Companion.DEFAULT,
    private val syntheticFiles: Collection<CjFile> = emptyList(),
    private val sdkDependency: M? = null,
    knownDependencyModuleDescriptors: Map<M, ModuleDescriptor> = emptyMap()
) : AbstractResolverForProject<M>(
    debugName,
    projectContext,
    listOf(module) + knownDependencyModuleDescriptors.keys,
    null,
    EmptyResolverForProject(),
    PackageOracleFactory.OptimisticFactory
) {
//    override fun sdkDependency(module: M): M? = sdkDependency

    init {
        knownDependencyModuleDescriptors.forEach { (module, descriptor) ->
            descriptorByModule[module] = ModuleData(
                descriptor as ModuleDescriptorImpl,
                (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
            )
        }
    }

    override fun modulesContent(module: M): ModuleContent<M> = when (module) {
        this.module -> ModuleContent(module, syntheticFiles, searchScope)
        else -> ModuleContent(module, emptyList(), searchScope)
    }

    override fun builtInsForModule(module: M): CangJieBuiltIns = builtIns

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: M): ResolverForModule =
        resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            modulesContent(moduleInfo),
            this,
            languageVersionSettings,
            CliSealedClassInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
}