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

package org.cangnova.cangjie.resolve.extensions

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.container.ComponentProvider
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.binding.BindingTrace

interface AnalysisHandlerExtension {

companion object{
    val EP_NAME = ExtensionPointName.create<AnalysisHandlerExtension>("org.cangnova.cangjie.analyzeComplete")
}
    fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<CjFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? = null

    fun analysisCompleted(
        project: Project,
        module: ModuleDescriptor,
        bindingTrace: BindingTrace,
        files: Collection<CjFile>
    ): AnalysisResult? = null
}
