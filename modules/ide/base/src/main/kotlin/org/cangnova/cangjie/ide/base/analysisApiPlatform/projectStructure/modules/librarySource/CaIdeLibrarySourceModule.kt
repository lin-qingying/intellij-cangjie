package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.librarySource

import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.library.CaIdeLibraryModule

/**
 * IDE 中的库源码模块。
 *
 * 库源码不是普通项目源码，它必须通过 [binaryLibraryModule] 与真实库产物绑定，
 * 否则 low-level resolve 会把“查看库源码”误判为“项目源码 use-site”。
 */
internal class CaIdeLibrarySourceModule(
    binaryLibraryModule: CaIdeLibraryModule,
) : CaIdeLibrarySourceModuleBase(
    binaryLibraryModule = binaryLibraryModule,
) {
    override val sourceRoots by lazy(LazyThreadSafetyMode.PUBLICATION) {
        binaryLibraryModule.computeRoots(LibraryRootTypeId.SOURCES)
    }

    override val stableModuleName: String
        get() = "ide-library-source:${binaryLibraryModule.entityId.presentableName}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CaIdeLibrarySourceModule && binaryLibraryModule == other.binaryLibraryModule
    }

    override fun hashCode(): Int {
        return binaryLibraryModule.hashCode()
    }
}
