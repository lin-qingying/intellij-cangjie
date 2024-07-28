package com.huawei.cangjie.cjpm.toolchain.wsl

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.toolchain.flavors.CjToolchainFlavor
import com.huawei.cangjie.ide.run.cjpm.runconfig.computeWithCancelableProgress
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import kotlin.io.path.isDirectory
import java.nio.file.InvalidPathException
import java.nio.file.Path

class CjWslToolchainFlavor : CjToolchainFlavor(){
    override fun getHomePathCandidates(): Sequence<Path> = sequence {
        val distributions = compute(CangJieBundle.message("progress.title.getting.installed.distributions")) {
            WslDistributionManager.getInstance().installedDistributions
        }
        for (distro in distributions) {
            yieldAll(distro.getHomePathCandidates())
        }
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
