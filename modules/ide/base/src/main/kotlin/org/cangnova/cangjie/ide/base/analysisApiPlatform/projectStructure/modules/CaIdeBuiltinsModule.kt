package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.presentableDescription

/**
 * IDE 中的内建模块。
 */
internal class CaIdeBuiltinsModule(
    project: Project,
    override val targetPlatform: TargetPlatform,
) : CaIdeMutableModule(project, { emptyList() }, includeLibrariesInScope = true), CaBuiltinsModule {
    override val builtinsName: String
        get() = "<ide-builtins>"

    override val stableModuleName: String
        get() = "ide-builtins:${targetPlatform.presentableDescription}"

    override val isResolvable: Boolean
        get() = false

    override val contentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override val moduleDescription: String
        get() = "IDE builtins (${targetPlatform.presentableDescription})"

    override fun equals(other: Any?): Boolean =
        other is CaBuiltinsModule && targetPlatform == other.targetPlatform

    override fun hashCode(): Int = targetPlatform.hashCode()
}
