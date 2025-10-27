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

package org.cangnova.cangjie.cjpm.actions

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.ui.actions.CjCommand
import org.cangnova.cangjie.project.ui.actions.CjCommandProvider

/**
 * CJPM 命令提供者
 *
 * 提供标准的 CJPM 命令实现，如 build、test、run 等。
 */
@Service(Service.Level.PROJECT)
class CjpmCommandProvider : CjCommandProvider {

    override fun getName(): String = "CJPM"

    override fun getHelpCommand(): String = "cjpm"

    override fun getAvailableCommands(project: Project, cjProject: CjProject?): List<CjCommand> {
        return listOf(
            CjCommand(
                id = "build",
                displayName = "Build",
                description = "Build the project",
                requiresArgs = false
            ),
            CjCommand(
                id = "test",
                displayName = "Test",
                description = "Run tests",
                requiresArgs = false
            ),
            CjCommand(
                id = "run",
                displayName = "Run",
                description = "Run the project",
                requiresArgs = false
            ),
            CjCommand(
                id = "clean",
                displayName = "Clean",
                description = "Clean build artifacts",
                requiresArgs = false
            ),
            CjCommand(
                id = "check",
                displayName = "Check",
                description = "Check code without building",
                requiresArgs = false
            ),
            CjCommand(
                id = "custom",
                displayName = "Custom Command",
                description = "Run a custom CJPM command",
                requiresArgs = true,
                argPrompt = "Enter CJPM command (e.g., build --release, test my_test):"
            )
        )
    }

    override fun executeCommand(project: Project, cjProject: CjProject, command: CjCommand, args: List<String>) {
        when (command.id) {
            "build" -> executeCjpmCommand(project, cjProject, listOf("build") + args)
            "test" -> executeCjpmCommand(project, cjProject, listOf("test") + args)
            "run" -> executeCjpmCommand(project, cjProject, listOf("run") + args)
            "clean" -> executeCjpmCommand(project, cjProject, listOf("clean") + args)
            "check" -> executeCjpmCommand(project, cjProject, listOf("check") + args)
            "custom" -> {
                if (args.isNotEmpty()) {
                    executeCjpmCommand(project, cjProject, args)
                } else {
                    Messages.showErrorDialog(project, "Please specify a command to execute.", "Error")
                }
            }
            else -> {
                Messages.showErrorDialog(project, "Unknown command: ${command.id}", "Error")
            }
        }
    }

    /**
     * 执行 CJPM 命令
     */
    private fun executeCjpmCommand(project: Project, cjProject: CjProject, command: List<String>) {
//        val workingDir = cjProject.rootDir.toNioPath().toFile()
//
//        val commandLine = GeneralCommandLine(command)
//            .withWorkDirectory(workingDir)
//            .withCharset(Charsets.UTF_8)
//
//        try {
//            val processHandler = ColoredProcessHandler(commandLine)
//            val console = CjpmConsole(project, "CJPM: ${command.joinToString(" ")}")
//            console.attachToProcess(processHandler)
//
//            // 启动进程
//            processHandler.startNotify()
//
//            // 等待进程完成
//            processHandler.waitFor()
//
//            val exitCode = processHandler.exitCode
//            if (exitCode != 0) {
//                Messages.showErrorDialog(
//                    project,
//                    "Command failed with exit code $exitCode",
//                    "Command Failed"
//                )
//            }
//
//        } catch (e: ExecutionException) {
//            Messages.showErrorDialog(
//                project,
//                "Failed to execute command: ${e.message}",
//                "Execution Error"
//            )
//        }
    }
}