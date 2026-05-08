package org.cangnova.cangjie.projectStructure

import com.intellij.codeInsight.multiverse.codeInsightContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope

/**
 * IDE 解析范围扩展器。
 *
 * 这里直接对齐 Kotlin `KotlinResolveScopeEnlarger`：
 * - 插件侧定义自己的扩展点；
 * - 复用 IntelliJ `ResolveScopeEnlarger` 对文件作用域扩展；
 * - 为 Analysis API source module 内容范围扩展提供模块级入口。
 */
interface CangJieResolveScopeEnlarger {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieResolveScopeEnlarger> =
            ExtensionPointName.create("org.cangnova.cangjie.resolveScopeEnlarger")

        fun enlargeScope(scope: GlobalSearchScope, file: PsiFile): GlobalSearchScope {
            val virtualFile = file.originalFile.virtualFile ?: return scope
            val context = file.originalFile.codeInsightContext

            var result = scope
            for (extension in ResolveScopeEnlarger.EP_NAME.extensions) {
                val project = scope.project ?: continue
                val additionalScope = extension.getAdditionalResolveScope(virtualFile, context, project) ?: continue
                result = result.union(additionalScope)
            }
            return result
        }

        fun enlargeScope(scope: GlobalSearchScope, module: Module, isTestScope: Boolean): GlobalSearchScope {
            var result = scope
            for (extension in EP_NAME.extensions) {
                val additionalScope = extension.getAdditionalResolveScope(module, isTestScope) ?: continue
                result = result.union(additionalScope)
            }
            return result
        }
    }

    fun getAdditionalResolveScope(module: Module, isTestScope: Boolean): SearchScope?
}
