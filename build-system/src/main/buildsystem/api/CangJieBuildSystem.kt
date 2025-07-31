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

package cn.cangnova.cangjie.buildsystem.api

import CangJieBuildResult
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTask

interface CangJieBuildSystem {

    companion object {
        val EP_NAME = ExtensionPointName.create<CangJieBuildSystem>("cn.cangnova.cangjie.buildSystem")
    }

    val id: String
    val name: String
    val description: String
    fun isApplicable(project: Project): Boolean
    fun isApplicable(project: Project, projectTask: ProjectTask): Boolean
    fun createBuilder(project: Project): CangJieProjectBuilder


}

// 枚举类，定义了三种构建模式：编译、重建、清理
enum class BuildMode { COMPILE, REBUILD, CLEAN }


enum class MessageLevel { INFO, WARNING, ERROR }

data class BuildError(
    val message: String,
    val file: VirtualFile? = null,
    val line: Int? = null,
    val column: Int? = null
)

data class BuildResult(
    val success: Boolean,
    val errors: List<BuildError> = emptyList()
)

interface BuildListener {
    fun onStart(mode: BuildMode)
    fun onProgress(message: String, percent: Int)
    fun onMessage(level: MessageLevel, message: String, file: VirtualFile?, line: Int?, column: Int?)
    fun onFinish(result: BuildResult)
}

interface CangJieProjectBuilder {
    fun build(context: CangJieCompileContext): CangJieBuildResult
    fun clean(context: CangJieCompileContext): CangJieBuildResult
}


interface CangJieCompileContext {
    val project: Project
    fun reportMessage(
        level: MessageLevel,
        message: String,
        file: VirtualFile? = null,
        line: Int? = null,
        column: Int? = null
    )

    fun reportProgress(message: String, percent: Int)
}
