/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.buildsystem.impl.cjpm.project.model.impl

import org.cangnova.cangjie.cjpm.project.model.CjpmProject
import org.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import org.cangnova.cangjie.cjpm.project.workspace.CjpmWorkspace
import org.cangnova.cangjie.utils.checkReadAccessAllowed
import org.cangnova.cangjie.utils.checkWriteAccessAllowed
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.LightDirectoryIndex
import java.util.*

class CjpmPackageIndex(
    private val project: Project,
    private val service: CjpmProjectsService
) : CjpmProjectsService.CjpmProjectsListener {
    private val indices: MutableMap<CjpmProject, LightDirectoryIndex<Optional<CjpmWorkspace.Package>>> = hashMapOf()
    private var indexDisposable: Disposable? = null

    init {
        project.messageBus.connect(project).subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, this)
    }


    override fun cjpmProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>) {
        checkWriteAccessAllowed()
        resetIndex()
        val disposable = Disposer.newDisposable("CjpmPackageIndexDisposable")
        Disposer.register(project, disposable)

        for (cjpmProject in projects) {
            val packages = cjpmProject.workspace?.packages.orEmpty()
            indices[cjpmProject] = LightDirectoryIndex(disposable, Optional.empty()) { index ->
                for (pkg in packages) {
                    val info = Optional.of(pkg)
                    index.putInfo(pkg.contentRoot, info)
//                    index.putInfo(pkg.outDir, info)
//                    for (additionalRoot in pkg.additionalRoots()) {
//                        index.putInfo(additionalRoot, info)
//                    }

                }
            }
        }
        indexDisposable = disposable

    }


    fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package? {
        checkReadAccessAllowed()
        val cjpmProject = service.findProjectForFile(file) ?: return null
        return indices[cjpmProject]?.getInfoForFile(file)?.orElse(null)
    }

    private fun resetIndex() {
        val disposable = indexDisposable
        if (disposable != null) {
            Disposer.dispose(disposable)
        }
        indexDisposable = null
        indices.clear()
    }
}
