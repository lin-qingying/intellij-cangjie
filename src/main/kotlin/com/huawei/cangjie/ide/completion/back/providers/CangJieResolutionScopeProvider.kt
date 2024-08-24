package com.huawei.cangjie.ide.completion.back.providers

import com.huawei.cangjie.analyzer.CjModule
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope


abstract class CangJieResolutionScopeProvider {
    public abstract fun getResolutionScope( module: CjModule): GlobalSearchScope
//    public abstract fun getResolutionScope(file:CjFile): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): CangJieResolutionScopeProvider =
            project.getService(CangJieResolutionScopeProvider::class.java)
    }
}
internal class IdeCangJieByModulesResolutionScopeProvider : CangJieResolutionScopeProvider() {

    override fun getResolutionScope(module: CjModule): GlobalSearchScope {
        return GlobalSearchScope.allScope(module.project)

    }

}
