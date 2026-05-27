@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieEmptyDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
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
        // IDE 宿主在 STUBS 模式下必须把库 stub 声明一起交给 declaration provider，
        // 否则 source -> `.cjo` / stdlib 的 reference binding 无法建立。
        val includeCompiledFiles =
            CaPlatformSettings.getInstance(project).deserializedDeclarationsOrigin == CaDeserializedDeclarationsOrigin.STUBS
        /*
         * 对齐 Kotlin `createDeclarationProvider(scope, contextualModule)` 的职责边界：
         * 当查询目标本身就是 builtins / library 模块里的 synthetic or binary 声明时，
         * provider 不能再被 use-site source scope 裁掉，否则 primitive builtin 这类
         * 没有源码 PSI 的声明将永远无法回到对应的 decompiled `.cjo` 视图。
         */
        val effectiveScope = when (contextualModule) {
            is CaBuiltinsModule,
            is CaLibraryModule,
                -> contextualModule.contentScope

            else -> scope
        }
        val files = CaIdeScopeCangJieFileCollector(project).collect(effectiveScope, includeCompiledFiles = includeCompiledFiles)
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
