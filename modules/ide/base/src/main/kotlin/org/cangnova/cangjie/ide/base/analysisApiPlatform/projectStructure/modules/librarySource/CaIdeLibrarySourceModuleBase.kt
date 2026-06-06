@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.librarySource

import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.CaIdeModuleContentScope
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library.CaIdeLibraryModule
import org.cangnova.cangjie.platform.TargetPlatform

/**
 * IDE 平台库源码模块基类。
 *
 * 对位 Kotlin `KaLibrarySourceModuleBase`：
 * - 公共展示名来自 backing binary library；
 * - 依赖直接镜像 backing binary library；
 * - 具体实现类只补 stable identity 与相等性。
 */
internal abstract class CaIdeLibrarySourceModuleBase(
    final override val binaryLibraryModule: CaIdeLibraryModule,
) : CaLibrarySourceModule, CaModuleBase() {
    final override val project
        get() = binaryLibraryModule.project

    override val libraryName: String
        get() = binaryLibraryModule.libraryName

    override val directRegularDependencies: List<CaModule>
        get() = binaryLibraryModule.directRegularDependencies

    override val directFriendDependencies: List<CaModule>
        get() = binaryLibraryModule.directFriendDependencies

    override val directDependsOnDependencies: List<CaModule>
        get() = binaryLibraryModule.directDependsOnDependencies

    override val transitiveDependsOnDependencies: List<CaModule>
        get() = binaryLibraryModule.transitiveDependsOnDependencies

    override val targetPlatform: TargetPlatform
        get() = binaryLibraryModule.targetPlatform

    final override val baseContentScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaIdeModuleContentScope(project, sourceRoots.mapNotNull { it.virtualFile }, includeLibrariesInScope = true)
    }

    abstract override val sourceRoots: List<PsiFileSystemItem>
}
