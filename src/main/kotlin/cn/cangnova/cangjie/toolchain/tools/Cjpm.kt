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

package cn.cangnova.cangjie.toolchain.tools


import cn.cangnova.cangjie.cjpm.CjpmConstants
import cn.cangnova.cangjie.cjpm.project.pathAsPath
import cn.cangnova.cangjie.cjpm.project.workspace.CjpmWorkspaceData
import cn.cangnova.cangjie.ide.experiments.CjExperiments
import cn.cangnova.cangjie.ide.module.CjProcessResult
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandLine
import cn.cangnova.cangjie.ide.run.cjpm.CjpmPatch
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.*
import cn.cangnova.cangjie.ide.run.isFeatureEnabled
import cn.cangnova.cangjie.lang.CjConstants.LIB_CJ_FILE
import cn.cangnova.cangjie.lang.CjConstants.MAIN_CJ_FILE
import cn.cangnova.cangjie.toolchain.parseSemVer
import cn.cangnova.cangjie.utils.buildList
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.SemVer
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.exists


fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}

//fun CjToolchainBase.cjpm(): Cjpm = Cjpm(this)
class Cjpm(
    toolchain: cn.cangnova.cangjie.toolchain.CjToolchainBase
) : CangJieComponent(NAME, toolchain) {

    init {
        toolchain.cjpm = this
    }

    fun checkNeedInstallCjpmGenerate(): Boolean {
        val crateName = "cjpm-generate"
        val minVersion = "0.53.4".parseSemVer()
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

        projectType: String? = null,

        ): CjProcessResult<GeneratedFilesHolder> = runWriteAction {


        val path = directory.pathAsPath
        val crateType = "--type=$projectType"
        val args = mutableListOf<String>()

        args.add(crateType)

        args.add("--name=$moduleName")


        CjpmCommandLine("init", path, args).execute(project, owner)
            .unwrapOrElse { return@runWriteAction CjResult.Err(it) }
        fullyRefreshDirectory(directory)

        val manifest =
            checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }


//        val manifest = checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        val fileName = MAIN_CJ_FILE
        val sourceFiles =
            listOfNotNull(directory.findFileByRelativePath("src/$fileName"))

        CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))

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

    /**
     * 获取所有依赖项并计算项目信息
     */
    fun fullProjectDescription(
        owner: Project,
        projectDirectory: Path,
        listenerProvider: (CjpmCallType) -> ProcessListener? = { null }
    ): CjResult<CjpmWorkspaceData, CjProcessExecutionOrDeserializationException> {
        val rawData = fetchMetadata(owner, projectDirectory, listener = listenerProvider(CjpmCallType.METADATA))
            .unwrapOrElse { return CjResult.Err(it) }
//        return CjResult.Ok(ProjectDescription(workspaceData, status))
        val workspaceData = cn.cangnova.cangjie.toolchain.impl.CjpmMetadata.clean(rawData)
        return CjResult.Ok(workspaceData)


    }

    fun fetchMetadata(
        owner: Project,
        projectDirectory: Path,

        toolchainOverride: String? = null,
        environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
        listener: ProcessListener?,

        ): CjResult<cn.cangnova.cangjie.toolchain.impl.CjpmMetadata.Project, CjProcessExecutionOrDeserializationException> {
        val rawData = fetchUpdate(owner, projectDirectory, listener = listener)

        return rawData
    }

    /**
     * 对cjpm项目进行更新
     */
    fun fetchUpdate(
        owner: Project,
        projectDirectory: Path,

        toolchainOverride: String? = null,
        environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
        listener: ProcessListener?,

        ): CjResult<cn.cangnova.cangjie.toolchain.impl.CjpmMetadata.Project, CjProcessExecutionOrDeserializationException> {

        val commandLine = CjpmCommandLine(
            command = "update",
            projectDirectory,
            toolchain = toolchainOverride,
            environmentVariables = environmentVariables
        )
        val output = commandLine.execute(
            owner, listener = listener
        ).unwrapOrElse { return CjResult.Err(it) }
        if (output.exitCode == 0 && output.stdout == "cjpm update success\n") {
            try {
                val project = readFile(projectDirectory)
                return CjResult.Ok(project)
            } catch (e: JacksonException) {
                return CjResult.Err(CjDeserializationException(e))
            } catch (e: IOException) {
                return CjResult.Err(CjModuleNotFound())
            }
        }

        return CjResult.Err(CjProcessExecutionException.Canceled(commandLine.command, output))

    }


    /**
     * 读取module-lock.json文件内容并序列化
     */
    private fun readFile(projectDirectory: Path): cn.cangnova.cangjie.toolchain.impl.CjpmMetadata.Project {
        val path = projectDirectory.resolve(CjpmConstants.LOCK_FILE)
//        val path = projectDirectory.resolve(CjpmConstantsService(toolchain = toolchain).LOCK_FILE)

        if (path.exists()) {
            val project = cn.cangnova.cangjie.toolchain.impl.CjpmMetadata.Project.serialization(path)
                .convertPaths(path.parent, toolchain::toLocalPath)
            return project


        }

        throw IOException("Can't find the module-lock.json file")
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

                )
//                .withEnvironment("CJC", cjcExecutable)

        }


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
        val JSON_MAPPER: ObjectMapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerKotlinModule()

        val TOML_MAPPER = ObjectMapper(TomlFactory()).apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        //        val TOML_MAPPER: ObjectMapper = TomlMapper().configure(TomlReadFeature.PARSE_JAVA_TIME, false)
//            .registerCangJieModule()
        const val NAME: String = "tools/bin/cjpm"

        @JvmStatic
        val TEST_NOCAPTURE_ENABLED_KEY: RegistryValue = Registry.get("cn.cangnova.cangjie.cjpm.test.nocapture")
        private val FEATURES_ACCEPTING_COMMANDS: List<String> = listOf(
            "update", "build", "check", "run-script", "clean", "run", "test", "publish", "list", "load", "init", "help"
        )

        fun getCjpmCommonPatch(project: Project): CjpmPatch = { it.patchArgs(project, true) }
        fun CjpmCommandLine.patchArgs(project: Project, colors: Boolean): CjpmCommandLine {
            val (pre, post) = splitOnDoubleDash()
                .let { (pre, post) -> pre.toMutableList() to post.toMutableList() }

//            if (command in listOf("test", "bench")) {
//                if (allFeatures && !pre.contains("--all-features")) {
//                    pre.add("--all-features")
//                }
//                if (TEST_NOCAPTURE_ENABLED_KEY.asBoolean() && !post.contains("--nocapture")) {
//                    post.add(0, "--nocapture")
//                }
//            }

//            if (requiredFeatures && command in FEATURES_ACCEPTING_COMMANDS) {
//                run {
//                    val cjpmProject = findCjpmProject(project, additionalArguments, workingDirectory) ?: return@run
//                    val cjpmPackage = findCjpmPackage(cjpmProject, additionalArguments, workingDirectory)
//                        ?: return@run
//                    if (workingDirectory != cjpmPackage.rootDirectory) {
//                        val manifestIdx = pre.indexOf("--manifest-path")
//                        val packageIdx = pre.indexOf("--package")
//                        if (manifestIdx == -1 && packageIdx != -1) {
//                            pre.removeAt(packageIdx) // remove `--package`
//                            pre.removeAt(packageIdx) // remove package name
//                            pre.add("--manifest-path")
//                            val manifest = cjpmPackage.rootDirectory.resolve(CjpmConstants.MANIFEST_FILE)
//                            pre.add(manifest.toAbsolutePath().toString())
//                        }
//                    }
//                    val cjpmTargets = findCjpmTargets(cjpmPackage, additionalArguments)
//                    val features = cjpmTargets.flatMap { it.requiredFeatures }.distinct().joinToString(",")
//                    if (features.isNotEmpty()) pre.add("--features=$features")
//                }
//            }

//            // Force colors
//            val forceColors = colors &&
//                    command in COLOR_ACCEPTING_COMMANDS &&
//                    additionalArguments.none { it.startsWith("--color") }
//            if (forceColors) pre.add(0, "--color=always")

            return copy(additionalArguments = if (post.isEmpty()) pre else pre + "--" + post)
        }
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

enum class CjpmCallType {
    METADATA,
    BUILD_SCRIPT_CHECK
}

enum class ProjectDescriptionStatus {
    BUILD_SCRIPT_EVALUATION_ERROR,
    OK
}

data class ProjectDescription(
    val workspaceData: CjpmWorkspaceData,
    val status: ProjectDescriptionStatus
)


