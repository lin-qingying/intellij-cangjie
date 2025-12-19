/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.projectStructure.scope.PoweredLibraryScopeBase

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
class CangJieSourceFilterScope private constructor(
    delegate: GlobalSearchScope,
    private val project: Project,
    private val filter: RootKindFilter
) : DelegatingGlobalSearchScope(delegate) {


    override fun contains(file: VirtualFile): Boolean {

        if (!super.contains(file)) {
            return false
        }

        return RootKindMatcher.Companion.matches(project, file, filter)
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
        fun everything(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.everything.copy())

        @JvmStatic
        fun projectAndLibrarySources(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.projectAndLibrarySourcesWithScripts.copy())

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
                delegate === EMPTY_SCOPE -> delegate
                delegate is CangJieSourceFilterScope -> CangJieSourceFilterScope(delegate.myBaseScope, project, filter)
                else -> CangJieSourceFilterScope(delegate, project, filter)
            }
        }

        @JvmStatic
        fun libraryClasses(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.libraryClasses)

        @JvmStatic
        fun libraryFiles(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.libraryFiles)

        @JvmStatic
        fun projectFiles(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.projectFiles.copy())

        @JvmStatic
        fun projectSourcesAndLibraryClasses(delegate: GlobalSearchScope, project: Project) =
            create(
                delegate,
                project,
                RootKindFilter.Companion.projectSourcesAndLibraryClasses.copy()
            )


        @JvmStatic
        fun projectSources(delegate: GlobalSearchScope, project: Project) =
            create(delegate, project, RootKindFilter.Companion.projectSources.copy())
    }

}
