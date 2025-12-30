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

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.utils.isCangJieFileType

internal class RootKindMatcherImpl(private val project: Project) : RootKindMatcher {
    private val fileIndex by lazy { ProjectRootManager.getInstance(project).fileIndex }


    override fun matches(filter: RootKindFilter, virtualFile: VirtualFile): Boolean {
        ProgressManager.checkCanceled()

        val cangjieExcludeLibrarySources = !filter.includeLibrarySourceFiles &&

                virtualFile.isCangJieFileType()

        if (cangjieExcludeLibrarySources && !filter.includeProjectSourceFiles) {
            return false
        }

        if (virtualFile !is VirtualFileWindow && fileIndex.isInSource(virtualFile)) {
            return filter.includeProjectSourceFiles
        }

        if (cangjieExcludeLibrarySources) {
            return false
        }
        val correctedFilter =
            filter.copy()



        if (correctedFilter.includeLibraryClassFiles/* && (isBinary || canContainClassFiles)*/) {
            if (fileIndex.isInLibraryClasses(virtualFile)) {
                return true
            }


        }

        if (correctedFilter.includeLibrarySourceFiles ) {
            if (fileIndex.isInLibrarySource(virtualFile)) {
                return true
            }


        }




        return false


    }
}
