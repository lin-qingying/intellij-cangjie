package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule

/**
 * IDE 中的 fallback 依赖模块。
 *
 * 该模块用于 outside-content-root / dangling-file 这类没有稳定内容根的 use-site 场景，
 * 只表达默认可见依赖边界，不伪装成真正源码模块。
 */
internal class CaIdeLibraryFallbackDependenciesModule(
    project: Project,
    override val dependencyOwnerName: String,
    private val ownerStableName: String,
) : CaIdeMutableModule(project, { emptyList() }, includeLibrariesInScope = true), CaLibraryFallbackDependenciesModule {
    override val stableModuleName: String
        get() = "$ownerStableName.fallback"

    override val isResolvable: Boolean
        get() = false

    override val moduleDescription: String
        get() = "IDE fallback dependencies of $dependencyOwnerName"
}
