package com.linqingying.cangjie.ide.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope


interface CangJieResolveScopeEnlarger {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieResolveScopeEnlarger> =
            ExtensionPointName.create("com.linqingying.cangjie.resolveScopeEnlarger")

        fun enlargeScope(scope: GlobalSearchScope, file: PsiFile): GlobalSearchScope {
            val virtualFile = file.originalFile.virtualFile ?: return scope

            var result = scope
            for (extension in ResolveScopeEnlarger.EP_NAME.extensions) {
                val project = scope.project ?: continue
                val additionalScope = extension.getAdditionalResolveScope(virtualFile, project) ?: continue
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
