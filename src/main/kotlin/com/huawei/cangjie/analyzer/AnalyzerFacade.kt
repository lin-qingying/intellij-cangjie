package com.huawei.cangjie.analyzer

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.config.LanguageVersionSettingsImpl
import com.huawei.cangjie.container.ComponentProvider
import com.huawei.cangjie.container.get
import com.huawei.cangjie.context.ModuleContext
import com.huawei.cangjie.descriptors.CompositePackageFragmentProvider
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentProvider
import com.huawei.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.huawei.cangjie.frontend.createContainerForLazyResolve
import com.huawei.cangjie.resolve.CodeAnalyzerInitializer
import com.huawei.cangjie.resolve.caches.ModuleContent
import com.huawei.cangjie.resolve.lazy.AbsentDescriptorHandler
import com.huawei.cangjie.resolve.lazy.ResolveSession
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker

interface TrackableModuleInfo : ModuleInfo {
    fun createModificationTracker(): ModificationTracker
}
fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
//    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}
//interface PackageOracleFactory {
//    fun createOracle(moduleInfo: ModuleInfo): PackageOracle
//
//    object OptimisticFactory : PackageOracleFactory {
//        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
//    }
//}
/**
 * Special-purpose module info that allows implementors to provide different behavior compared to the [originalModule]'s.
 * E.g. may be used to resolve common code as if it were target-specific, or to change the dependencies visible to the code.
 *
 * Resolvers should accept a derived module info, iff the [originalModule] is accepted.
 */
interface DerivedModuleInfo : ModuleInfo {
    val originalModule: ModuleInfo
}

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(moduleInfo: M) = diagnoseUnknownModuleInfo(listOf(moduleInfo))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>) =
        throw IllegalStateException("Should not be called for $infos")

//    override fun moduleInfoForModuleDescriptor(moduleDescriptor: ModuleDescriptor): M {
//        throw IllegalStateException("$moduleDescriptor is not contained in this resolver")
//    }
}

abstract class ResolverForProject<M : ModuleInfo> {
    abstract val allModules: Collection<M>
    fun resolverForModule(moduleInfo: M): ResolverForModule = resolverForModuleDescriptor(descriptorForModule(moduleInfo))

    abstract val name: String
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    abstract fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing

    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?

}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}
abstract class ResolverForModuleFactory {
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
//        sealedInheritorsProvider: SealedClassInheritorsProvider,
//        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
//            sealedInheritorsProvider,
//            resolveOptimizingOptions
        )
    }

    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
//        sealedInheritorsProvider: SealedClassInheritorsProvider,
//        resolveOptimizingOptions: OptimizingOptions?,
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
//            sealedInheritorsProvider
            null
        )
    }


}


class CangJieResolverForModuleFactory:ResolverForModuleFactory(){
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {

        val project = moduleContext.project
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


        val container = createContainerForLazyResolve(

            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
//            moduleClassResolver,
//            targetEnvironment,
//            lookupTracker,
//            ExpectActualTracker.DoNothing,
//            InlineConstTracker.DoNothing,
//            EnumWhenTracker.DoNothing,
//            packagePartProvider,
            languageVersionSettings,
//            sealedInheritorsProvider = sealedInheritorsProvider,
//            useBuiltInsProvider = platformParameters.useBuiltinsProviderForModule(moduleInfo),
//            optimizingOptions = resolveOptimizingOptions,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )
        val providersForModule = arrayListOf(
            container.get<ResolveSession>().getPackageFragmentProvider(),

        )
        return ResolverForModule(
            CompositePackageFragmentProvider(providersForModule, "CompositeProvider@JvmResolver for $moduleDescriptor"),
            container
        )
    }
}
interface LanguageSettingsProvider {
    fun getLanguageVersionSettings(
        moduleInfo: ModuleInfo,
        project: Project
    ): LanguageVersionSettings


    object Default : LanguageSettingsProvider {
        override fun getLanguageVersionSettings(
            moduleInfo: ModuleInfo,
            project: Project
        ) = LanguageVersionSettingsImpl.DEFAULT

             }
}
