package com.huawei.cangjie.analyzer

import com.huawei.cangjie.descriptors.ModuleCapability
import com.huawei.cangjie.ide.cache.cacheByClassInvalidatingOnRootModifications
import com.huawei.cangjie.ide.projectStructure.CangJieModuleDependencyCollector
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServices
import com.huawei.cangjie.resolve.PlatformDependentAnalyzerServicesImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.psi.search.GlobalSearchScope

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

    val contentScope: GlobalSearchScope

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
    open val library: LibraryEx,
) : ModuleInfo {
//    override fun checkValidity() {
//        if (isDisposed) {
//            throw AlreadyDisposedException("Library '${name}' is already disposed")
//        }
//    }
}

class CangJieLibraryInfo(override val project: Project, library: LibraryEx) : LibraryInfo(project, library) {
    override val name: Name = Name.special("<sources for library ${library.name}>")
    override val module: Module
        get() = TODO("Not yet implemented")
    override val moduleOrigin: ModuleOrigin = ModuleOrigin.LIBRARY
    override val contentScope: GlobalSearchScope
        get() = TODO("Not yet implemented")
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

    //        override val contentScope: GlobalSearchScope
//        get() = CangJieResolveScopeEnlarger.enlargeScope(
//            module.moduleProductionSourceScope,
//            module,
//            isTestScope = false
//        )
    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.allScope(project)

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
