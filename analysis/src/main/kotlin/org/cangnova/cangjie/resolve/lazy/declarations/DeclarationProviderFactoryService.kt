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

package org.cangnova.cangjie.resolve.lazy.declarations

import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.psi.psiUtil.sure
import java.util.ArrayList

class CliDeclarationProviderFactoryService(private val sourceFiles: Collection<CjFile>) :
    DeclarationProviderFactoryService() {

    override fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<CjFile>,
        filesScope: GlobalSearchScope,
        context: AnalysisContext
    ): DeclarationProviderFactory {
        val allFiles = ArrayList<CjFile>()
        sourceFiles.filterTo(allFiles) {
            val vFile = it.virtualFile.sure { "Source files should be physical files" }
            filesScope.contains(vFile)
        }
        allFiles.addAll(syntheticFiles)
        return FileBasedDeclarationProviderFactory(storageManager, allFiles)
    }
}

abstract class DeclarationProviderFactoryService {
    abstract fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<CjFile>,
        filesScope: GlobalSearchScope,
        context: AnalysisContext

    ): DeclarationProviderFactory

    companion object {
        @JvmStatic
        fun createDeclarationProviderFactory(
            project: Project,
            storageManager: StorageManager,
            syntheticFiles: Collection<CjFile>,
            moduleContentScope: GlobalSearchScope,
            context: AnalysisContext

        ): DeclarationProviderFactory {
            return project.getService(DeclarationProviderFactoryService::class.java)!!
                .create(
                    project,
                    storageManager,
                    syntheticFiles,
                    filteringScope(syntheticFiles, moduleContentScope),
                    moduleInfo
                )
        }

        private fun filteringScope(
            syntheticFiles: Collection<CjFile>,
            baseScope: GlobalSearchScope
        ): GlobalSearchScope {
            if (syntheticFiles.isEmpty() || baseScope == GlobalSearchScope.EMPTY_SCOPE) {
                return baseScope
            }
            return SyntheticFilesFilteringScope(syntheticFiles, baseScope)
        }
    }

    private class SyntheticFilesFilteringScope(syntheticFiles: Collection<CjFile>, baseScope: GlobalSearchScope) :
        DelegatingGlobalSearchScope(baseScope) {

        private val originals = syntheticFiles.mapNotNullTo(HashSet<VirtualFile>()) { it.originalFile.virtualFile }

        override fun contains(file: VirtualFile) = super.contains(file) && file !in originals

        override fun toString() = "SyntheticFilesFilteringScope($myBaseScope)"
    }
}
