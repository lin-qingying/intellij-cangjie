package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.analyzer.*
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.createBuiltIns
import com.linqingying.cangjie.context.ProjectContext
import com.linqingying.cangjie.context.withModule
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.impl.ModuleDescriptorImpl

import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.IdePackageOracleFactory
import com.linqingying.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope

data class ModuleContent<out M : ModuleInfo>(
    val moduleInfo: M,
    val syntheticFiles: Collection<CjFile>,
    val moduleContentScope: GlobalSearchScope
)


class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    modules: Collection<ModuleInfo>,
    private val syntheticFilesByModule: Map<ModuleInfo, Collection<CjFile>>,
    delegateResolver: ResolverForProject<ModuleInfo>,
    fallbackModificationTracker: ModificationTracker? = null,

    ) : AbstractResolverForProject<ModuleInfo>(

    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    projectContext.project.service<IdePackageOracleFactory>()
) {

    private val builtInsCache: BuiltInsCache =
        (delegateResolver as? IdeaResolverForProject)?.builtInsCache ?: BuiltInsCache(projectContext, this)


    private fun getResolverForModuleFactory(moduleInfo: ModuleInfo): ResolverForModuleFactory {


        return CangJieResolverForModuleFactory()
    }
    override fun modulesContent(module: ModuleInfo): ModuleContent<ModuleInfo> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.moduleContentScope)

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: ModuleInfo): ResolverForModule {
        val moduleContent =
            ModuleContent(moduleInfo, syntheticFilesByModule[moduleInfo] ?: listOf(), moduleInfo.moduleContentScope)

        val project = projectContext.project
        val languageVersionSettings =
            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(moduleInfo, project)

        val resolverForModuleFactory = getResolverForModuleFactory(moduleInfo)
        val optimizingOptions = ResolveOptimizingOptionsProvider.getOptimizingOptions(project, descriptor, moduleInfo)

        val resolverForModule = resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            moduleContent,
            this,
            languageVersionSettings,
            sealedInheritorsProvider = IdeSealedClassInheritorsProvider,
            resolveOptimizingOptions = optimizingOptions,
            absentDescriptorHandlerClass = IdeaAbsentDescriptorHandler::class.java
        )
//        ResolverForModuleComputationTrackerEx.getInstance(project)?.onCreateResolverForModule(descriptor, moduleInfo)
        return resolverForModule
    }

    override fun builtInsForModule(module: ModuleInfo): CangJieBuiltIns {


        return builtInsCache.getOrCreateIfNeeded(module)
    }
    // Important: ProjectContext must be from SDK to be sure that we won't run into deadlocks


    class BuiltInsCache(private val projectContext: ProjectContext, private val resolver: IdeaResolverForProject) {
        private val cache = mutableMapOf<BuiltInsCacheKey, CangJieBuiltIns>()

        fun getOrCreateIfNeeded(module: ModuleInfo): CangJieBuiltIns = projectContext.storageManager.compute {
            ProgressManager.checkCanceled()

//            val sdk = resolverForSdk.sdkDependency(module)
//            val stdlib = findStdlibForModulesBuiltins(module)

            val key = module.getKeyForBuiltIns()
            val cachedBuiltIns = cache[key]
            if (cachedBuiltIns != null) return@compute cachedBuiltIns


            createBuiltIns(projectContext, resolver)
                .also {
                    // TODO: MemoizedFunction should be used here instead, but for proper we also need a module (for LV settings) that is not contained in the key
                    cache[key] = it
                }
        }

//        private fun findStdlibForModulesBuiltins(module: ModuleInfo): CjpmLibraryInfo? {
//            return when (IdeBuiltInsLoadingState.state) {
//                IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_CLASSLOADER -> null
//                IdeBuiltInsLoadingState.IdeBuiltInsLoading.FROM_DEPENDENCIES_JVM -> {
//                    if (module.platform.isJvm()) {
//                        module.findJvmStdlibAcrossDependencies()
//                    } else {
//                        null
//                    }
//                }
//            }
//        }
    }


}
