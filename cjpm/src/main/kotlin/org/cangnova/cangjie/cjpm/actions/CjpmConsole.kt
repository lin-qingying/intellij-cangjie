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

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project

/**
 * 简单的 CJPM 控制台实现
 *
 * 用于显示 CJPM 命令的输出结果
 */
//class CjpmConsole(project: Project, title: String) {
//
//    fun attachToProcess(processHandler: ProcessHandler) {
//        processHandler.addProcessListener(object : ProcessAdapter() {
//            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//                // 简单的输出处理，可以根据需要扩展
//                println(event.text.trim())
//            }
//
//            override fun processTerminated(event: ProcessEvent) {
//                val exitCode = event.exitCode
//                val message = if (exitCode == 0) {
//                    "Process finished successfully with exit code $exitCode"
//                } else {
//                    "Process finished with exit code $exitCode (ERROR)"
//                }
//                println(message)
//            }
//        })
//    }
//}