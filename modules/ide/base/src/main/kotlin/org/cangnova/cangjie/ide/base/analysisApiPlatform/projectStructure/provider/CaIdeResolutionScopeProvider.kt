@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * IDE 平台 resolution-scope 服务。
 *
 * 解析作用域的真相与模块图、内容范围一样都来自 [CaIdeProjectStructureState]，
 * 这里保持与其它平台 service 一致的委托边界。
 */
class CaIdeResolutionScopeProvider(
    project: Project,
) : CaResolutionScopeProvider {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override fun getResolutionScope(module: CaModule): CaResolutionScope {
        return state.getResolutionScope(module)
    }
}
