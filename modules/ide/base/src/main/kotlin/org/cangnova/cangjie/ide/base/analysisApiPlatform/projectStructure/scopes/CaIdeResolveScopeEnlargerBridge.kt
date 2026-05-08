@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.scopes

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source.CaIdeSourceModule
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import org.cangnova.cangjie.projectStructure.CangJieResolveScopeEnlarger

/**
 * 把插件侧 `CangJieResolveScopeEnlarger` 桥接到 Analysis API content-scope 细化路径。
 */
internal class CaIdeResolveScopeEnlargerBridge : CaContentScopeRefiner {
    override fun getEnlargementScopes(module: CaModule): List<GlobalSearchScope> {
        val sourceModule = module as? CaIdeSourceModule ?: return emptyList()

        return listOf(
            CangJieResolveScopeEnlarger.enlargeScope(
                GlobalSearchScope.EMPTY_SCOPE,
                sourceModule.openapiModule,
                isTestScope = sourceModule.kind == CaSourceModuleKind.TEST,
            ),
        )
    }
}
