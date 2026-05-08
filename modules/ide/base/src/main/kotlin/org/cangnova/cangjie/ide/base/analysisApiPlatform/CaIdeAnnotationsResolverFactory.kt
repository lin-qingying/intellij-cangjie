@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolverFactory
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjAnnotated

/**
 * IDE 平台默认注解解析器工厂。
 *
 * 当前项目尚未引入独立的插件注解索引实现。
 * 在没有 `CangJieCompilerPluginsProvider` 的平台上，空解析器是正确语义，
 * 它保证 low-level session 构造链完整，但不会伪造任何插件注解事实。
 */
class CaIdeAnnotationsResolverFactory : CangJieAnnotationsResolverFactory {
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): CangJieAnnotationsResolver {
        return CaEmptyAnnotationsResolver
    }
}

private object CaEmptyAnnotationsResolver : CangJieAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<CjAnnotated> = emptySet()

    override fun annotationsOnDeclaration(declaration: CjAnnotated): Set<ClassId> = emptySet()
}
