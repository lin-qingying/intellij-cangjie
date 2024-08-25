package com.huawei.cangjie.ide.base.projectStructure

import com.huawei.cangjie.ide.projectStructure.scope.PoweredLibraryScopeBase
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


        @JvmStatic
        fun createByType(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {

            return when (delegate) {
                is PoweredLibraryScopeBase -> {
//                    不知晓出于何种原因，使用DelegatingGlobalSearchScope 无法找的位于库的索引
                    delegate
//                    create(
//                        delegate, project, RootKindFilter(
//                            true,
//
//                            true,
//
//                            true,
//
//                            true,
//
//                            true,
//
//                            true,
//                        )
//                    )
                }

                else -> {
                    projectSources(delegate, project)
                }
            }

        }

        fun create(
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
        fun libraryClasses(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.libraryClasses)

        @JvmStatic
        fun libraryFiles(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.libraryFiles)

        @JvmStatic
        fun projectFiles(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.projectFiles.copy(includeScriptsOutsideSourceRoots = true))

        @JvmStatic
        fun projectSourcesAndLibraryClasses(delegate: GlobalSearchScope, project: Project) =
            create(
                delegate,
                project,
                RootKindFilter.projectSourcesAndLibraryClasses.copy(includeScriptsOutsideSourceRoots = true)
            )


        @JvmStatic
        fun projectSources(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.projectSources.copy(includeScriptsOutsideSourceRoots = true))
    }

}
