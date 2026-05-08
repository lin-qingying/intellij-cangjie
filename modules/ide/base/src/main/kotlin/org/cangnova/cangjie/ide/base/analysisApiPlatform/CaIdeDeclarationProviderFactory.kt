@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieEmptyDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * IDE 平台的主声明 provider 工厂。
 *
 * 组织方式参考 Kotlin `IdeKotlinDeclarationProviderFactory`：
 * 工厂只负责基于作用域构造 provider，不把 XML 注册、包查询、注解查询揉到同一个实现中。
 */
class CaIdeDeclarationProviderFactory(
    private val project: Project,
) : CangJieDeclarationProviderFactory {
    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider {
        val files = CaIdeScopeCangJieFileCollector(project).collect(scope)
        if (files.isEmpty()) return CangJieEmptyDeclarationProvider

        return CangJieCompositeDeclarationProvider.create(files.map(::CangJieFileBasedDeclarationProvider))
    }
}

/**
 * IDE 平台的声明 provider merger。
 */
class CaIdeDeclarationProviderMerger : CangJieDeclarationProviderMerger {
    override fun merge(providers: List<CangJieDeclarationProvider>): CangJieDeclarationProvider {
        return CangJieCompositeDeclarationProvider.create(providers)
    }
}
