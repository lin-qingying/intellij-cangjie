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

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

data class RootKindFilter(
    val includeProjectSourceFiles: Boolean,
    val includeLibraryClassFiles: Boolean,
    val includeLibrarySourceFiles: Boolean,

    val includeResources: Boolean
) {


    companion object {

        @JvmField
        val projectAndLibrarySources = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = true,

            includeResources = false
        )

        @JvmField
        val projectAndLibrarySourcesWithScripts = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = true,

            includeResources = false
        )

        @JvmField
        val projectSourcesAndLibraryClasses = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,

            includeResources = false
        )

        @JvmField
        val projectFiles = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,

            includeResources = false
        )

        @JvmField
        val projectSourcesAndResources = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = false,

            includeResources = true
        )

        @JvmField
        val librarySources = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = true,

            includeResources = false
        )

        @JvmField
        val everything = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = true,

            includeResources = false
        )

        @JvmField
        val libraryFiles = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = true,

            includeResources = false
        )

        @JvmField
        val libraryClasses = RootKindFilter(
            includeProjectSourceFiles = false,
            includeLibraryClassFiles = true,
            includeLibrarySourceFiles = false,

            includeResources = false
        )

        @JvmField
        val projectSources = RootKindFilter(
            includeProjectSourceFiles = true,
            includeLibraryClassFiles = false,
            includeLibrarySourceFiles = false,

            includeResources = false
        )
    }
}

fun RootKindFilter.matches(element: PsiElement): Boolean {
    return RootKindMatcher.matches(element, this)
}

fun RootKindFilter.matches(project: Project, virtualFile: VirtualFile): Boolean {
    return RootKindMatcher.matches(project, virtualFile, this)
}

interface RootKindMatcher {
    fun matches(filter: RootKindFilter, virtualFile: VirtualFile): Boolean

    companion object {
        @JvmStatic
        fun matches(project: Project, virtualFile: VirtualFile, filter: RootKindFilter): Boolean {
            val matcherService = project.service<RootKindMatcher>()
            return matcherService.matches(filter, virtualFile)
        }

        @JvmStatic
        fun matches(element: PsiElement, filter: RootKindFilter): Boolean {
            val virtualFile = when (element) {
                is PsiDirectory -> element.virtualFile
                is PsiFile -> element.virtualFile
                else -> element.containingFile?.virtualFile
            }

            return if (virtualFile != null) matches(element.project, virtualFile, filter) else false
        }
    }

}
