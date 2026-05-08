@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaExperimentalApi::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.scopes

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieGlobalSearchScopeMergeStrategy
import kotlin.reflect.KClass

internal class CaIdeUnionScopeMergeStrategy : CangJieGlobalSearchScopeMergeStrategy<GlobalSearchScope> {
    override val targetType: KClass<GlobalSearchScope> = GlobalSearchScope::class

    override fun uniteScopes(scopes: List<GlobalSearchScope>): List<GlobalSearchScope> {
        return scopes.flatMapTo(linkedSetOf()) { scope -> GlobalSearchScopeUtil.flattenUnionScope(scope) }.toList()
    }
}
