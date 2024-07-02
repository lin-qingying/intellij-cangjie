package com.huawei.cangjie.idea.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
class CangJieSourceFilterScope private constructor(
    delegate: GlobalSearchScope,
    private val project: Project,
    private val filter: RootKindFilter
) : DelegatingGlobalSearchScope(delegate) {

    override fun getProject() = project

    override fun contains(file: VirtualFile): Boolean {
        val baseScope = this.myBaseScope
        if (!super.contains(file)) {
            return false
        }

        return RootKindMatcher.matches(project, file, filter)
    }

    override fun toString(): String {
        return "CangJieSourceFilterScope(delegate=$myBaseScope, filter=$filter)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        other as CangJieSourceFilterScope

        if (filter != other.filter) return false
        if (project != other.project) return false

        return true
    }

    override fun calcHashCode(): Int {
        var result = super.calcHashCode()
        result = 31 * result + filter.hashCode()
        result = 31 * result + project.hashCode()
        return result
    }


    companion object {


        private fun create(
            delegate: GlobalSearchScope,
            project: Project,
            filter: RootKindFilter
        ): GlobalSearchScope {
            return when {
                delegate === GlobalSearchScope.EMPTY_SCOPE -> delegate
                delegate is CangJieSourceFilterScope -> CangJieSourceFilterScope(delegate.myBaseScope, project, filter)
                else -> CangJieSourceFilterScope(delegate, project, filter)
            }
        }


        @JvmStatic
        fun projectSources(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.projectSources.copy(includeScriptsOutsideSourceRoots = true))
    }

}