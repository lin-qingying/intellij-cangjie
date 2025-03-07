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

package cn.cangnova.cangjie.analyzer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

abstract class ProjectStructureProvider {

    /**
     * Returns a [CjModule] for a given [element] in the context of the [contextualModule].
     *
     * The contextual module is the [CjModule] from which [getModule] is called. It is a way to disambiguate the [CjModule] of [element]s
     * with whom multiple modules might be associated. In particular:
     *
     *  1. It allows replacing the original [CjModule] of [element] with another module, e.g. for supporting outsider files (see below).
     *  2. It helps to distinguish between multiple possible [CjModule]s for library elements.
     *
     * #### Outsider Modules
     *
     * Normally, every CangJie source file either belongs to some module (e.g. a source module, or a library module), or is self-contained
     * (a script file, or a file outside content roots). However, in certain cases there might be special modules that include both
     * existing source files, and also some additional files.
     *
     * An example of such a module is one that owns an 'outsider' source file. Outsiders are used in IntelliJ for displaying files that
     * technically belong to some module, but are not included in the module's content roots (e.g. a file from a previous VCS revision).
     * As there might be cross-references between the outsider file and other files in the module, they need to be analyzed as a single
     * synthetic module. Inside an analysis session for such a module (which would be the [contextualModule]), sources that originally
     * belong to a source module should be treated rather as a part of the synthetic one.
     */
    abstract fun getModule(element: PsiElement, contextualModule: CjModule?): CjModule

    companion object{
        fun getInstance(project: Project): ProjectStructureProvider {
            return project.getService(ProjectStructureProvider::class.java)
        }

        fun getModule(project: Project, element: PsiElement, contextualModule: CjModule?): CjModule {
            return getInstance(project).getModule(element, contextualModule)
        }
    }
}

internal class ProjectStructureProviderIdeImpl(private val project: Project) : ProjectStructureProvider() {
    override fun getModule(element: PsiElement, contextualModule: CjModule?): CjModule {
        return DefaultCjModule(project)
    }
}
