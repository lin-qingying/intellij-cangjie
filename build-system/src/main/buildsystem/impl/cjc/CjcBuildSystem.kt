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

package org.cangnova.cangjie.buildsystem.impl.cjc

import org.cangnova.cangjie.buildsystem.api.BuildMode
import org.cangnova.cangjie.buildsystem.api.CangJieBuildSystem
import org.cangnova.cangjie.buildsystem.api.CangJieCompileContext
import org.cangnova.cangjie.buildsystem.api.CangJieProjectBuilder
import org.cangnova.cangjie.buildsystem.state.BuildSettingsState
import org.cangnova.cangjie.toolchain.CangJieToolchain
import com.intellij.openapi.module.Module.ELEMENT_TYPE
import com.intellij.openapi.project.Project
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectTask

class CjcBuildSystem : CangJieBuildSystem {
    override val id: String
        get() = "CJC"
    override val name: String
        get() = "CJC"
    override val description: String
        get() = "CJC"

    override fun isApplicable(project: Project): Boolean {
        return BuildSettingsState.getInstance(project).defaultBuildSystemId == id && CangJieToolchain.getToolchain().isAvailable

    }

    override fun isApplicable(project: Project, projectTask: ProjectTask): Boolean {
        return projectTask is ModuleBuildTask && projectTask.module.getOptionValue(ELEMENT_TYPE) == "CANGJIE_MODULE" && BuildSettingsState.getInstance(
            project
        ).defaultBuildSystemId == id && CangJieToolchain.getToolchain().isAvailable

    }

    override fun toString(): String {
        return name
    }

    override fun createBuilder(project: Project): CangJieProjectBuilder {

        return CjcProjectBuilder(project)
    }
}

internal class CjcProjectBuilder(val project: Project):CangJieProjectBuilder {
    override fun build(context: CangJieCompileContext): CangJieBuildResult {
        TODO("Not yet implemented")
    }

    override fun clean(context: CangJieCompileContext): CangJieBuildResult {
        TODO("Not yet implemented")
    }

}