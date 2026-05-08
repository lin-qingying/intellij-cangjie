@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieGlobalSearchScopeMergeStrategy
import org.cangnova.cangjie.projectStructure.scope.CombinableSourceAndClassRootsScope
import org.cangnova.cangjie.projectStructure.scope.CombinedSourceAndClassRootsScope
import kotlin.reflect.KClass

internal class CaIdeCombinableSourceAndClassRootsScopeMergeStrategy(
    private val project: Project,
) : CangJieGlobalSearchScopeMergeStrategy<CombinableSourceAndClassRootsScope> {
    override val targetType: KClass<CombinableSourceAndClassRootsScope> = CombinableSourceAndClassRootsScope::class

    override fun uniteScopes(scopes: List<CombinableSourceAndClassRootsScope>): List<GlobalSearchScope> {
        @Suppress("UNCHECKED_CAST")
        return when {
            scopes.size <= 1 -> scopes as List<GlobalSearchScope>
            else -> listOf(CombinedSourceAndClassRootsScope.create(scopes, project))
        }
    }
}
