package com.huawei.cangjie.analyzer

import com.huawei.cangjie.cjpm.project.workspace.CjpmLibrary
import com.huawei.cangjie.descriptors.ModuleCapability
import com.huawei.cangjie.ide.cache.cacheByClassInvalidatingOnRootModifications
import com.huawei.cangjie.ide.projectStructure.CangJieModuleDependencyCollector
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServices
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServicesImpl
import com.intellij.diagnostic.ActivityCategory
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.impl.ModuleEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.MessageBus
import java.nio.file.Path


enum class ModuleOrigin {
    MODULE,
    LIBRARY,
    OTHER
}

interface ModuleInfo {

    val name: Name
    val project: Project
    val module: Module?
    val displayedName: String get() = name.asString()

    val moduleOrigin: ModuleOrigin

    //    val contentScope: GlobalSearchScope
    val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.allScope(project)


    val moduleContentScope: GlobalSearchScope
        get() = contentScope

    fun dependencies(): List<ModuleInfo> {
        return emptyList()
    }

    val analyzerServices: PlatformDependentAnalyzerServices
    fun dependencyOnBuiltIns(): DependencyOnBuiltIns = analyzerServices.dependencyOnBuiltIns()

    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
    enum class DependencyOnBuiltIns { NONE, AFTER_SDK, LAST }

    companion object {
        val Capability = ModuleCapability<ModuleInfo>("ModuleInfo")
    }
}

abstract class LibraryInfo internal constructor(
    override val project: Project,
    open val cjpmLibrary: CjpmLibrary,
    open val library: LibraryEx? = null,
) : ModuleInfo {
//    override fun checkValidity() {
//        if (isDisposed) {
//            throw AlreadyDisposedException("Library '${name}' is already disposed")
//        }
//    }
}

class CangJieLibraryInfo(override val project: Project, library: CjpmLibrary) : LibraryInfo(project, library) {
    override val name: Name = Name.special("<sources for library ${library.name}>")
    override val module: Module
        get() {
//         TODO   该模块在不依赖此库时并没有卸载
            return ModuleManager.getInstance(project)
                .findModuleByName(this.cjpmLibrary.sourceRoots.first().toNioPath().toFile().name)
                ?: ModuleManager.getInstance(project).modules[0]

        }
    override val moduleOrigin: ModuleOrigin = ModuleOrigin.LIBRARY

    // TODO 依赖
//    override val contentScope: GlobalSearchScope
//        get() = GlobalSearchScope.moduleScope(module)
//    override val contentSc/ope: GlobalSearchScope
//        get() = CangJieResolveScopeEnlarger.enlargeScope(
//            module.moduleProductionSourceScope,
//            module,
//            isTestScope = false
//        )

    //    LibraryWithoutSourceScope(project, library)
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = TODO("Not yet implemented")


}

data class CangJieModuleInfo(

    override val module: Module
) : ModuleInfo, DerivedModuleInfo {


    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    override val name: Name = Name.special("<production sources for module ${module.name}>")
    override val project: Project = module.project
    override val originalModule: ModuleInfo
        get() = this

//    override val contentScope: GlobalSearchScope
//        get() = CangJieResolveScopeEnlarger.enlargeScope(
//            module.moduleProductionSourceScope,
//            module,
//            isTestScope = false
//        )
//override val contentScope: GlobalSearchScope
//    get() = GlobalSearchScope.moduleScope(module)
//    override val contentScope: GlobalSearchScope
//        get() = GlobalSearchScope.allScope(project)

    override fun dependencies(): List<ModuleInfo> {
        return module.cacheByClassInvalidatingOnRootModifications(this::class.java) {

            CangJieModuleDependencyCollector.getInstance(module.project)
                .collectModuleDependencies(module, includeExportedDependencies = true)
                .toList()
        }

//        TODO 暂时返回空
//        return emptyList()
    }

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = PlatformDependentAnalyzerServicesImpl

}


class a : ModuleEx {
    override fun <T : Any?> getUserData(key: Key<T>): T? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun getExtensionArea(): ExtensionsArea {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getComponent(interfaceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    override fun hasComponent(interfaceClass: Class<*>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isInjectionForExtensionSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getMessageBus(): MessageBus {
        TODO("Not yet implemented")
    }

    override fun isDisposed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getDisposed(): Condition<*> {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getService(serviceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClass(aClass: Class<T>, pluginId: PluginId): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClass(className: String, pluginDescriptor: PluginDescriptor): T & Any {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClassWithConstructorInjection(
        aClass: Class<T>,
        key: Any,
        pluginId: PluginId
    ): T {
        TODO("Not yet implemented")
    }

    override fun createError(error: Throwable, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(message: String, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(
        message: String,
        error: Throwable?,
        pluginId: PluginId,
        attachments: MutableMap<String, String>?
    ): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> loadClass(className: String, pluginDescriptor: PluginDescriptor): Class<T> {
        TODO("Not yet implemented")
    }

    override fun getActivityCategory(isExtension: Boolean): ActivityCategory {
        TODO("Not yet implemented")
    }

    override fun getModuleFile(): VirtualFile? {
        TODO("Not yet implemented")
    }

    override fun getModuleNioFile(): Path {
        TODO("Not yet implemented")
    }

    override fun getProject(): Project {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun isLoaded(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setOption(key: String, value: String?) {
        TODO("Not yet implemented")
    }

    override fun getOptionValue(key: String): String? {
        TODO("Not yet implemented")
    }

    override fun getModuleScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleScope(includeTests: Boolean): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleWithLibrariesScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleWithDependenciesScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleContentScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleContentWithDependenciesScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleWithDependenciesAndLibrariesScope(includeTests: Boolean): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleWithDependentsScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleTestsWithDependentsScope(): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun getModuleRuntimeScope(includeTests: Boolean): GlobalSearchScope {
        TODO("Not yet implemented")
    }

    override fun clearScopesCache() {
        TODO("Not yet implemented")
    }

    override fun getDeprecatedModuleLevelMessageBus(): MessageBus {
        TODO("Not yet implemented")
    }
}
