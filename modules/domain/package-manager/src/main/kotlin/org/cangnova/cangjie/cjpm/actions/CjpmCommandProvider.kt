/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.actions

import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.ui.actions.CjCommand
import org.cangnova.cangjie.project.ui.actions.CjCommandProvider
import javax.swing.Icon

/**
 * CJPM 命令提供者
 *
 * 提供标准的 CJPM 命令实现，如 build、test、run 等。
 */

internal class CjpmCommandProvider : CjCommandProvider {
    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

    override fun getName(): String = "CJPM"

    override fun getHelpCommand(): String = "cjpm"

    override fun getAvailableCommands(project: Project, cjProject: CjProject?): List<CjCommand> {
        return CjpmCommands.entries
    }

    override fun executeCommand(project: Project, cjProject: CjProject, command: CjCommand, args: List<String>) {
        TODO("命令执行器还未实现")
    }

    override fun getIcon(value: String): Icon = CangJieIcons.CANGJIE

    override fun createRunAnythingItem(command: String, icon: Icon): RunAnythingItem =
        CjpmRunAnythingItem(command, icon)

    override fun getHelpDescription(): String = "Runs CJPM command"

    override fun getCompletionGroupTitle(): String = "CJPM commands"

    override fun getHelpGroupTitle(): String = "CJPM"

    /**
     * 执行 CJPM 命令
     */
    private fun executeCjpmCommand(project: Project, cjProject: CjProject, command: List<String>) {

    }
}


data class CjpmOption(val name: String, val description: String, val double: Boolean = true) {
    val longName: String
        get() = if (double) {
            "--$name"
        } else {
            "-$name"
        }
}

enum class CjpmCommands(override val description: String, val options: List<CjpmOption>) : CjCommand {

    BUILD(
        description = "Compile the current package",
        options = listOf(
            CjpmOption("incremental", """增量编译"""),
            CjpmOption("verbose", """用来展示编译日志"""),
            CjpmOption("g", """生成 debug 版本的输出产物""", false),
            CjpmOption("coverage", """生成覆盖率信息，默认情况下不开启覆盖率功能"""),
            CjpmOption("jobs ", """用来指定并行编译的最大并发数，最终的最大并发数取 N 和 2倍 CPU 核数 的最小值"""),
            CjpmOption("condition=\"\"", """按条件透传 module.json 中的命令"""),
            CjpmOption("build-dir ", """"指定输出产物的存放路径"""),
            CjpmOption("output ", """"指定输出可执行文件的名称"""),
            CjpmOption(
                "targetPlatform ",
                """"交叉编译代码到目标平台，cjpm.toml 中的配置可参考 cross_compile_configuration 部分"""
            ),
            CjpmOption("help", """帮助"""),

            CjpmOption(
                "lint ",
                """"在编译时调用仓颉语言静态检查工具 cjlint 进行代码检查，如果检查到【要求】级别的代码规范违规，则编译会失败，检查到【建议】级别的违规仅报警，正常完成编译"""
            ),


            )
    ),

    CHECK(
        description = "Check the current package",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),

    CLEAN(
        description = "Remove generated artifacts",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),


    RUN(
        description = "Run the current package",
        options = listOf(
            CjpmOption("help", """帮助"""),

            CjpmOption("name", """指定运行的二进制名称"""),
            CjpmOption("build-args", """<value> 控制cjpm编译流程的参数"""),
            CjpmOption("skip-build", """跳过编译流程，直接运行"""),
            CjpmOption("run-args", """透传参数给本次运行的二进制产物"""),
            CjpmOption("g", """用来运行debug版本的产物""", false),
            CjpmOption("verbose", """用来展示运行日志"""),

            )
    ),


    TEST(
        description = "Execute unit and integration tests of a package",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),


    UPDATE(
        description = "Update dependencies as recorded in the local lock file",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),
    LOAD(
        description = "Update dependencies as recorded in the local lock file",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),

    INIT(
        description = "Create a new Cjpm package in an existing directory",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),

    RUN_SCRIPT(
        description = "Build and install a CangJie binary",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),


    PUBLISH(
        description = "Upload a package to the registry",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    ),


    HELP(
        description = "Get help for a Cjpm command",
        options = emptyList(

        )
    ),

    VERSION(
        description = "Show version information",
        options = listOf(
            CjpmOption("help", """帮助"""),

            )
    );

    override val id: String get() = presentableName
    override val displayName: String get() = presentableName
    override val icon: String? get() = null
    override val requiresArgs: Boolean get() = options.isNotEmpty()
    override val argPrompt: String? get() = if (requiresArgs) "Options: ${options.joinToString(", ") { it.longName }}" else null

    override fun getOptions(): Map<String, String> = options.associate { it.longName to it.description }

    val presentableName: String get() = name.lowercase().replace('_', '-')
}
