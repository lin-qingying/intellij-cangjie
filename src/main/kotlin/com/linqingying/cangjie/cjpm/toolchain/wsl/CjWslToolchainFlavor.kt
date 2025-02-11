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

package com.linqingying.cangjie.cjpm.toolchain.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath.Companion.isWslUncPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.toolchain.flavors.CjToolchainFlavor
import com.linqingying.cangjie.ide.run.cjpm.runconfig.computeWithCancelableProgress
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isDirectory


fun Path.hasExecutableOnWsl(toolName: String): Boolean = pathToExecutableOnWsl(toolName).toFile().isFile
fun Path.pathToExecutableOnWsl(toolName: String): Path = resolve(toolName)

class CjWslToolchainFlavor : CjToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> = sequence {
        val distributions = compute(CangJieBundle.message("progress.title.getting.installed.distributions")) {
            WslDistributionManager.getInstance().installedDistributions
        }
        for (distro in distributions) {
            yieldAll(distro.getHomePathCandidates())
        }
    }

    override fun pathToExecutable(path: Path, toolName: String): Path {
        return path.pathToExecutableOnWsl(toolName)


    }
    override fun hasExecutable(path: Path, toolName: String): Boolean {
        return path.hasExecutableOnWsl(toolName)

    }

    override fun isValidToolchainPath(path: Path): Boolean {
        return isWslUncPath(path.toString()) && super.isValidToolchainPath(path)

    }

    override fun isApplicable(): Boolean {

        return WSLUtil.isSystemCompatible()
    }
}

fun WSLDistribution.getHomePathCandidates(): Sequence<Path> = sequence {
    @Suppress("UnstableApiUsage", "UsePropertyAccessSyntax")
    val root = getUNCRootPath()
    val environment = compute(CangJieBundle.message("progress.title.getting.environment.variables")) { environment }
    if (environment != null) {
        val home = environment["HOME"]
        val remoteCjpmPath = home?.let { "$it/.cangjie/bin" }
        val localCjpmPath = remoteCjpmPath?.let { root.resolve(it) }
        if (localCjpmPath?.isDirectory() == true) {
            yield(localCjpmPath)
        }

        val sysPath = environment["PATH"]
        for (remotePath in sysPath.orEmpty().split(":")) {
            if (remotePath.isEmpty()) continue
            val localPath = root.resolveOrNull(remotePath) ?: continue
            if (!localPath.isDirectory()) continue
            yield(localPath)
        }
    }

    for (remotePath in listOf("/usr/local/bin", "/usr/bin")) {
        val localPath = root.resolve(remotePath)
        if (!localPath.isDirectory()) continue
        yield(localPath)
    }
}

val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread

/**
 * 在指定上下文中计算给定的任务，并根据当前线程决定是否显示进度对话框。
 *
 * @param title 进度对话框的标题，仅在UI线程执行时显示。
 * @param getter 一个无参数的函数，用于执行计算任务。
 * @return T 计算任务的结果，类型由调用者指定。
 *
 * 此函数根据当前线程是否是UI线程来决定是否显示进度对话框。
 * 如果是UI线程，则通过ProjectManager获取默认项目，并使用该项目的实例
 * 来显示一个可取消的进度对话框，在此对话框中执行给定的任务。
 * 如果不是UI线程，则直接执行给定的任务，不显示进度对话框。
 */
private fun <T> compute(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    getter: () -> T
): T = if (isDispatchThread) {
    val project = ProjectManager.getInstance().defaultProject
    project.computeWithCancelableProgress(title, getter)
} else {
    getter()
}


fun Path.resolveOrNull(other: String): Path? = pathOrNull { resolve(other) }

private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {

        null
    }
}
