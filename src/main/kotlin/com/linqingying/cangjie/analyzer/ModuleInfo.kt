/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.workspace.CjpmLibrary
import com.linqingying.cangjie.descriptors.ModuleCapability
import com.linqingying.cangjie.ide.base.projectStructure.CangJieSourceFilterScope
import com.linqingying.cangjie.ide.base.projectStructure.RootKindFilter
import com.linqingying.cangjie.ide.cache.cacheByClassInvalidatingOnRootModifications
import com.linqingying.cangjie.ide.projectStructure.CangJieModuleDependencyCollector
import com.linqingying.cangjie.ide.projectStructure.CangJieResolveScopeEnlarger
import com.linqingying.cangjie.ide.projectStructure.scope.PoweredLibraryScopeBase
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.PlatformDependentAnalyzerServices
import com.linqingying.cangjie.resolve.PlatformDependentAnalyzerServicesImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope


enum class ModuleOrigin {
    MODULE,
    LIBRARY,
    OTHER
}

interface ModuleInfo {

    val name: Name
    val project: Project
    val contentScope: GlobalSearchScope
//        get() = GlobalSearchScope.allScope(project)


    val moduleContentScope: GlobalSearchScope
        get() = contentScope

    //    val module: Module
    val displayedName: String get() = name.asString()

    val moduleOrigin: ModuleOrigin

    //    val contentScope: GlobalSearchScope


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

interface ModuleSourceInfo : ModuleInfo {
    val module: Module

}
fun  ModuleInfo.isLibraryClasses() =   this is LibraryInfo

abstract class LibraryInfo internal constructor(
    override val project: Project,
    val library: LibraryEx,

    ) : ModuleSourceInfo {

    override val moduleOrigin: ModuleOrigin get() = ModuleOrigin.LIBRARY
    override val name: Name = Name.special("<library ${library.name}>")
    override val displayedName: String
        get() = CangJieBundle.message("library.0", library.presentableName)

        override val contentScope: GlobalSearchScope
        get() = LibraryWithoutSourceScope(project, library)
//    override val contentScope: GlobalSearchScope
//        get() = CangJieSourceFilterScope.create(LibraryWithoutSourceScope(project, library), project
//             ,   RootKindFilter(
//                 true,
//
//                true,
//
//                true,
//
//                true,
//
//                true,
//
//                true,
//             )
//        )
    override val module: Module
        get() {

            project.modules.forEach {
                if (it.name == project.name)
                    return it

            }
            return project.modules[0]

        }

    override fun dependencies(): List<ModuleInfo> {
//        val dependencies = LibraryDependenciesCache.getInstance(project).getLibraryDependencies(this)
        return buildList {
            add(this@LibraryInfo)
//            addAll(dependencies.sdk)
//            addAll(dependencies.librariesWithoutSelf)
        }
    }

    override fun toString() =
        "${this::class.simpleName}@${Integer.toHexString(System.identityHashCode(this))}($library)"


}


class CangJieLibrary(override val project: Project, library: LibraryEx) : LibraryInfo(project, library) {

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = PlatformDependentAnalyzerServicesImpl
}

abstract class CjpmLibraryInfo internal constructor(
    override val project: Project,
    open val cjpmLibrary: CjpmLibrary,

    ) : ModuleInfo {
//    override fun checkValidity() {
//        if (isDisposed) {
//            throw AlreadyDisposedException("Library '${name}' is already disposed")
//        }
//    }
}

//class CangJieCjpmLibraryInfo(override val project: Project, library: CjpmLibrary) : CjpmLibraryInfo(project, library) {
//    override val name: Name = Name.special("<sources for library ${library.name}>")
//
//    //    override val module: Module
////        get() {
//////         TODO   该模块在不依赖此库时并没有卸载
////            return ModuleManager.getInstance(project)
////                .findModuleByName(this.cjpmLibrary.sourceRoots.first().toNioPath().toFile().name)
////                ?: ModuleManager.getInstance(project).modules[0]
////
////        }
//    override val moduleOrigin: ModuleOrigin = ModuleOrigin.LIBRARY
////    override fun dependencies(): List<ModuleInfo> {
////        return module.cacheByClassInvalidatingOnRootModifications(this::class.java) {
////
////
////            listOf(this)
//////            CangJieModuleDependencyCollector.getInstance(module.project)
//////                .collectModuleDependencies(module, includeExportedDependencies = true)
//////                .toList()
////        }
////    }
//    // TODO 依赖
////    override val contentScope: GlobalSearchScope
////        get() = GlobalSearchScope.moduleScope(module)
////    override val contentSc/ope: GlobalSearchScope
////        get() = CangJieResolveScopeEnlarger.enlargeScope(
////            module.moduleProductionSourceScope,
////            module,
////            isTestScope = false
////        )
//
//    //    LibraryWithoutSourceScope(project, library)
//    override val analyzerServices: PlatformDependentAnalyzerServices = PlatformDependentAnalyzerServicesImpl
//
////    override val contentScope: GlobalSearchScope
////        get() = CangJieResolveScopeEnlarger.enlargeScope(
////            module.moduleProductionSourceScope,
////            module,
////            isTestScope = false
////        )
//}

data class CangJieModuleInfo(

    override val module: Module
) : ModuleSourceInfo, DerivedModuleInfo {


    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    override val name: Name = Name.special("<production sources for module ${module.name}>")
    override val project: Project = module.project
    override val originalModule: ModuleInfo
        get() = this

    override val contentScope: GlobalSearchScope
        get() = CangJieResolveScopeEnlarger.enlargeScope(
            module.moduleProductionSourceScope,
            module,
            isTestScope = false
        )
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

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class LibraryWithoutSourceScope(
    project: Project,
    private val library: Library
) : PoweredLibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), VirtualFile.EMPTY_ARRAY) {

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getClassRootForFile(file)

    override fun equals(other: Any?) = other is LibraryWithoutSourceScope && library == other.library
    override fun calcHashCode(): Int = library.hashCode()
    override fun toString() = "LibraryWithoutSourceScope($library)"
}

fun ModuleInfo.projectSourceModules(): List<ModuleSourceInfo> {
    return when (this) {
        is ModuleSourceInfo -> listOf(this)
//        is PlatformModuleInfo -> containedModules
        else -> emptyList()
    }
}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide 'calcHashCode()'
private class LibrarySourceScope(
    project: Project,
    private val library: Library
) : PoweredLibraryScopeBase(project, VirtualFile.EMPTY_ARRAY, library.getFiles(OrderRootType.SOURCES)) {
    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getSourceRootForFile(file)

    override fun equals(other: Any?) = other is LibrarySourceScope && library == other.library
    override fun calcHashCode(): Int = library.hashCode()
    override fun toString() = "LibrarySourceScope($library)"
}
