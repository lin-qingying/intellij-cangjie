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

/**
 * 将路径转换为系统相关的可执行文件路径。
 * 在 Windows 系统中，会在路径末尾添加 `.exe` 后缀。
 *
 * @return 系统相关的可执行文件路径
 *
 * 示例：
 * ```kotlin
 * "cjc".toSystemIndependentPath()
 * // Windows: "cjc.exe"
 * // Linux/Mac: "cjc"
 * ```
 */
fun String.toSystemIndependentPath(): String {
    return if (SystemInfo.isWindows) {
        "$this.exe"
    } else {
        this
    }
}

/**
 * 将下划线命名（snake_case）转换为大驼峰命名（PascalCase）。
 * 将字符串按下划线分割，然后将每个单词的首字母大写。
 *
 * @return 转换后的大驼峰命名字符串
 *
 * 示例：
 * ```kotlin
 * "hello_world_test".toCamelCase() // 返回 "HelloWorldTest"
 * "my_class".toCamelCase()         // 返回 "MyClass"
 * ```
 */
fun String.toCamelCase(): String {
    return this.split('_')
        .joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }
}

/**
 * 判断当前运行的操作系统是否为 Windows。
 *
 * @return 如果是 Windows 系统返回 true，否则返回 false
 */
fun isWindows(): Boolean {
    val os = System.getProperty("os.name")
    return os.lowercase(Locale.getDefault()).startsWith("win")
}

/**
 * 在 IntelliJ 终端中异步执行命令。
 * 命令会在后台线程池中执行，并监听执行过程的各个阶段。
 *
 * @param command 要执行的命令字符串，多个参数用空格分隔
 *
 * 该方法会监听以下事件：
 * - 命令开始执行
 * - 命令执行结束及退出码
 * - 命令的标准输出和错误输出
 *
 * 示例：
 * ```kotlin
 * executeCommand("git status")
 * executeCommand("npm install")
 * ```
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

/**
 * 检查当前是否持有写入访问权限。
 * IntelliJ Platform 要求对 PSI、VFS 等进行修改操作时必须持有写入锁。
 *
 * @throws IllegalStateException 如果当前没有写入访问权限
 *
 * 示例：
 * ```kotlin
 * checkWriteAccessAllowed()
 * // 执行修改操作...
 * ```
 */
fun checkWriteAccessAllowed() {
    check(ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Needs write action"
    }
}

/**
 * 检查当前是否持有读取访问权限。
 * IntelliJ Platform 要求访问 PSI、VFS 等时必须持有读取锁。
 *
 * @throws IllegalStateException 如果当前没有读取访问权限
 *
 * 示例：
 * ```kotlin
 * checkReadAccessAllowed()
 * // 执行读取操作...
 * ```
 */
fun checkReadAccessAllowed() {
    check(ApplicationManager.getApplication().isReadAccessAllowed) {
        "Needs read action"
    }
}

/**
 * 显示指定类型的设置对话框。
 * 使用 reified 类型参数自动查找对应的配置页面。
 *
 * @param T 配置页面类型，必须实现 [Configurable] 接口
 *
 * 示例：
 * ```kotlin
 * project.showSettingsDialog<MyProjectConfigurable>()
 * ```
 */
inline fun <reified T: Configurable> Project.showSettingsDialog() {
    ShowSettingsUtil.getInstance().showSettingsDialog(this, T::class.java)
}


