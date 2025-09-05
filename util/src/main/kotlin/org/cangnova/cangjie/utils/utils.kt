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

package org.cangnova.cangjie.utils


import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import java.util.*

fun String.toSystemIndependentPath(): String {
    return if (SystemInfo.isWindows) {
        "$this.exe"
    } else {
        this
    }
}

/**
 * 将下划线命名转换为大驼峰命名
 */

fun String.toCamelCase(): String {
    return this.split('_')
        .joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

/**
 * 判断系统是否为windows
 */
fun isWindows(): Boolean {
    val os = System.getProperty("os.name")
    return os.lowercase(Locale.getDefault()).startsWith("win")
}

/**
 * 在intellij终端执行命令
 */

fun executeCommand(command: String) {
    val commandLine = GeneralCommandLine(command.split(" "))
    val processHandler = OSProcessHandler(commandLine)

    processHandler.addProcessListener(object : ProcessListener {
        override fun startNotified(event: ProcessEvent) {
            println("Command started: $command")
        }

        override fun processTerminated(event: ProcessEvent) {
            println("Command finished with exit code: ${event.exitCode}")
        }

        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            println("Output: ${event.text}")
        }
    })

    ApplicationManager.getApplication().executeOnPooledThread {
        processHandler.startNotify()
    }
}

fun checkWriteAccessAllowed() {
    check(ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Needs write action"
    }
}
fun checkReadAccessAllowed() {
    check(ApplicationManager.getApplication().isReadAccessAllowed) {
        "Needs read action"
    }
}

inline fun <reified T: Configurable> Project.showSettingsDialog() {
    ShowSettingsUtil.getInstance().showSettingsDialog(this, T::class.java)
}


