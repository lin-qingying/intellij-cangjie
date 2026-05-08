package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule

/**
 * IDE 中的内建模块。
 */
internal class CaIdeBuiltinsModule(
    project: Project,
) : CaIdeMutableModule(project, { emptyList() }, includeLibrariesInScope = true), CaBuiltinsModule {
    override val builtinsName: String
        get() = "<ide-builtins>"

    override val stableModuleName: String
        get() = "ide-builtins"

    override val isResolvable: Boolean
        get() = false

    override val contentScope: GlobalSearchScope
        get() = CaBuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override val moduleDescription: String
        get() = "IDE builtins"
}
