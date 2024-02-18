package com.huawei.cangjie.cjpm.toolchain.tools

import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.parseSemVer
import com.huawei.cangjie.idea.experiments.CjExperiments
import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.CjProcessResult
import com.huawei.cangjie.idea.run.cjpm.CjpmCommandLine
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjCapturingProcessHandler
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessExecutionException
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjResult
import com.huawei.cangjie.idea.run.isFeatureEnabled
import com.huawei.cangjie.lang.CjConstants.LIB_CJ_FILE
import com.huawei.cangjie.lang.CjConstants.MAIN_CJ_FILE

import com.huawei.cangjie.utils.buildList
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.text.SemVer


fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}

fun CjToolchainBase.cjpm(): Cjpm = Cjpm(this)
class Cjpm(
    toolchain: CjToolchainBase
) : CangJieComponent(NAME, toolchain) {
    fun checkNeedInstallCjpmGenerate(): Boolean {
        val crateName = "cjpm-generate"
        val minVersion = "0.45.2".parseSemVer()
        return checkBinaryCrateIsNotInstalled(crateName, minVersion)
    }

    private fun checkBinaryCrateIsNotInstalled(crateName: String, minVersion: SemVer?): Boolean {
        val installed = listInstalledBinaryCrates().any { (name, version) ->
            name == crateName && (minVersion == null || version != null && version >= minVersion)
        }
        return !installed
    }

    data class GeneratedFilesHolder(val manifest: VirtualFile, val sourceFiles: List<VirtualFile>)

    private fun listInstalledBinaryCrates(): List<BinaryCrate> =
        createBaseCommandLine("list")
            .execute(toolchain.executionTimeoutInMilliseconds)
            ?.stdoutLines
            ?.filterNot { it.startsWith(" ") }
            ?.mapNotNull { BinaryCrate.from(it) }
            .orEmpty()

    fun CjpmCommandLine.patchArgs(project: Project): CjpmCommandLine {

        val (pre, post) = splitOnDoubleDash()
            .let { (pre, post) -> pre.toMutableList() to post.toMutableList() }

//        TODO 对参数进行处理(添加或者删除)

        return copy(additionalArguments = if (post.isEmpty()) pre else pre + "--" + post)

    }


    fun init(
        project: Project,
        owner: Disposable,
        directory: VirtualFile,
        name: String,
        moduleName: String = name,
        organizationName: String = name,
        projectType: String? = null
    ): CjProcessResult<GeneratedFilesHolder> {
        val path = directory.pathAsPath
        val crateType = "--type=$projectType"

        val args = mutableListOf(crateType, moduleName, organizationName)

        CjpmCommandLine("init", path, args).execute(project, owner).unwrapOrElse { return CjResult.Err(it) }
        fullyRefreshDirectory(directory)
        val manifest = checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        val fileName = MAIN_CJ_FILE
        val sourceFiles = listOfNotNull(directory.findFileByRelativePath("src/$fileName"))
        return CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))
    }

    fun init(
        project: Project,
        owner: Disposable,
        directory: VirtualFile,
        name: String,
        createBinary: Boolean,
        vcs: String? = null
    ): CjProcessResult<GeneratedFilesHolder> {
        val path = directory.pathAsPath
        val crateType = "--type=${
            if (createBinary) "executable" else "static"
        }"

        val args = mutableListOf(crateType, name, name)
//
//        vcs?.let {
//            args.addAll(listOf("--vcs", vcs))
//        }

//        args.add(path.toString())

        CjpmCommandLine("init", path, args).execute(project, owner).unwrapOrElse { return CjResult.Err(it) }
        fullyRefreshDirectory(directory)

        val manifest = checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        val fileName = if (createBinary) MAIN_CJ_FILE else LIB_CJ_FILE
        val sourceFiles = listOfNotNull(directory.findFileByRelativePath("src/$fileName"))
        return CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))
    }


    fun generate(
        project: Project,
        owner: Disposable,
        directory: VirtualFile,
        name: String,
        templateUrl: String,
        vcs: String? = null
    ): CjProcessResult<GeneratedFilesHolder> {
        val path = directory.pathAsPath
        val args = mutableListOf(
            "--name", name,
            "--git", templateUrl,
            "--init",
            "--force"
        )

        vcs?.let {
            args.addAll(listOf("--vcs", vcs))
        }

        CjpmCommandLine("generate", path, args)
            .execute(project, owner)
            .unwrapOrElse { return CjResult.Err(it) }
        fullyRefreshDirectory(directory)

        val manifest = checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        val sourceFiles = listOf("main", "lib").mapNotNull { directory.findFileByRelativePath("src/${it}.cj") }
        return CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))
    }


    private fun CjpmCommandLine.execute(
        project: Project,
        owner: Disposable = project,
        stdIn: ByteArray? = null,
        listener: ProcessListener? = null
    ): CjProcessResult<ProcessOutput> {
        return toGeneralCommandLine(project, copy(emulateTerminal = false)).execute(owner, stdIn, listener = listener)
    }

    fun toGeneralCommandLine(project: Project, commandLine: CjpmCommandLine): GeneralCommandLine =
        with(commandLine.patchArgs(project)) {
            val parameters = buildList {
                when {

                    toolchain != null -> add("+$toolchain")
                }

                add(command)
                addAll(additionalArguments)
            }


//            val cjcExecutable = this@Cjpm.toolchain.cjc().executable.toString()
            this@Cjpm.toolchain.createGeneralCommandLine(
                executable,
                workingDirectory,
                redirectInputFrom,

                environmentVariables,
                parameters,
                emulateTerminal,

                if (isFeatureEnabled(CjExperiments.BUILD_TOOL_WINDOW)) withSudo else false,
                http = http
            )
//                .withEnvironment("CJC", cjcExecutable)

        }

    private var _http: HttpConfigurable? = null

    private val http: HttpConfigurable
        get() = _http ?: HttpConfigurable.getInstance()

    fun checkSupportForBuildCheckAllTargets(): Boolean {
        val lines = createBaseCommandLine("help", "check")
            .execute(toolchain.executionTimeoutInMilliseconds)
            ?.stdoutLines
            ?: return false
        return lines.any { it.contains(" --all-targets ") }
    }

    data class BinaryCrate(val name: String, val version: SemVer? = null) {
        companion object {

            private val VERSION_LINE: Regex = """(?<name>[\w-]+) v(?<version>\d+\.\d+\.\d+(-[\w.]+)?).*""".toRegex()

            fun from(line: String): BinaryCrate? {
                val result = VERSION_LINE.matchEntire(line) ?: return null
                val name = result.groups["name"]?.value ?: return null
                val rawVersion = result.groups["version"]?.value ?: return null
                return BinaryCrate(name, SemVer.parseFromText(rawVersion))
            }
        }
    }

    companion object {

        const val NAME: String = "tools/bin/cjpm"

        @JvmStatic
        val TEST_NOCAPTURE_ENABLED_KEY: RegistryValue = Registry.get("com.huawei.cangjie.cjpm.test.nocapture")
        private val FEATURES_ACCEPTING_COMMANDS: List<String> = listOf(
            "update", "build", "check", "run-script", "clean", "run", "test", "publish", "list", "load", "init", "help"
        )
    }

}

inline fun <T, E> CjResult<T, E>.unwrapOrElse(op: (E) -> T): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> op(err)
}

fun GeneralCommandLine.execute(
    owner: Disposable,
    stdIn: ByteArray? = null,
    runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress(timeoutInMilliseconds = null) },
    listener: ProcessListener? = null
): CjProcessResult<ProcessOutput> {


    val handler = CjCapturingProcessHandler.startProcess(this) // The OS process is started here
        .unwrapOrElse {

            return CjResult.Err(CjProcessExecutionException.Start(commandLineString, it))
        }

    val cjpmKiller = Disposable {

        if (!handler.isProcessTerminated) {
            handler.process.destroyForcibly() // Send SIGKILL
            handler.destroyProcess()
        }
    }

    val alreadyDisposed = runReadAction {
        if (Disposer.isDisposed(owner)) {
            true
        } else {
            Disposer.register(owner, cjpmKiller)
            false
        }
    }

    if (alreadyDisposed) {
        Disposer.dispose(cjpmKiller) // Kill the process

        val output = ProcessOutput().apply { setCancelled() }
        return CjResult.Err(CjProcessExecutionException.Canceled(commandLineString, output, "Command failed to start"))
    }

    listener?.let { handler.addProcessListener(it) }

    val output = try {
        if (stdIn != null) {
            handler.processInput.use { it.write(stdIn) }
        }

        handler.runner()
    } finally {
        Disposer.dispose(cjpmKiller)
    }

    return when {
        output.isCancelled -> CjResult.Err(CjProcessExecutionException.Canceled(commandLineString, output))
        output.isTimeout -> CjResult.Err(CjProcessExecutionException.Timeout(commandLineString, output))
        output.exitCode != 0 -> CjResult.Err(CjProcessExecutionException.ProcessAborted(commandLineString, output))
        else -> CjResult.Ok(output)
    }
}