@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaGlobalSearchScopeMerger
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieGlobalSearchScopeMergeStrategy
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieIntersectionScopeMergeTarget
import kotlin.reflect.KClass

internal class CaIdeIntersectionScopeMergeStrategy(
    private val project: Project,
) : CangJieGlobalSearchScopeMergeStrategy<GlobalSearchScope> {
    override val targetType: KClass<GlobalSearchScope> = GlobalSearchScope::class

    override fun uniteScopes(scopes: List<GlobalSearchScope>): List<GlobalSearchScope> {
        if (!scopes.any(GlobalSearchScopeUtil::isIntersectionScope)) return scopes

        val (intersectionScopes, restScopes) = scopes.partition(GlobalSearchScopeUtil::isIntersectionScope)
        return uniteIntersectionScopes(intersectionScopes) + restScopes
    }

    private fun uniteIntersectionScopes(scopes: List<GlobalSearchScope>): List<GlobalSearchScope> {
        if (scopes.size < 2) return scopes

        val scopesByMergeTargets = linkedMapOf<Set<GlobalSearchScope>, MutableList<GlobalSearchScope>>()

        scopes.forEach { scope ->
            val componentScopes = GlobalSearchScopeUtil.flattenIntersectionScope(scope)
            val (mergeTargets, remainingScopes) = componentScopes.partition { it is CangJieIntersectionScopeMergeTarget }
            val nonFactoredScope = intersectAll(remainingScopes)
            scopesByMergeTargets.getOrPut(mergeTargets.toSet()) { mutableListOf() } += nonFactoredScope
        }

        val globalSearchScopeMerger = CaGlobalSearchScopeMerger.getInstance(project)
        return scopesByMergeTargets.entries.flatMap { (mergeTargets, nonFactoredScopes) ->
            if (mergeTargets.isEmpty()) return@flatMap nonFactoredScopes

            val mergedNonFactoredScope = globalSearchScopeMerger.union(nonFactoredScopes)
            listOf(intersectAll(mergeTargets.toList() + mergedNonFactoredScope))
        }
    }

    private fun intersectAll(scopes: List<GlobalSearchScope>): GlobalSearchScope {
        if (scopes.isEmpty()) return GlobalSearchScope.everythingScope(project)
        return scopes.reduce { acc, scope -> acc.intersectWith(scope) }
    }
}
