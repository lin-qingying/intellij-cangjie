//package com.huawei.cangjie.idea.run.cjpm.back
//
//import com.huawei.cangjie.idea.project.settings.CangJieProjectSettingsService
//import com.huawei.cangjie.idea.run.cjpm.CjToolchainBase
//import com.huawei.cangjie.idea.run.cjpm.back.Cjpm.Companion.LOG
//import com.huawei.cangjie.idea.run.cjpm.CjpmCommandLine
//import com.huawei.cangjie.idea.run.cjpm.GeneralCommandLine
//import com.huawei.cangjie.idea.run.cjpm.runconfig.CjCapturingProcessHandler
//import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessExecutionException
//import com.huawei.cangjie.idea.run.cjpm.runconfig.unwrapOrElse
//import com.huawei.cangjie.idea.run.cjpm.withWorkDirectory
//import com.intellij.execution.configurations.GeneralCommandLine
//import com.intellij.execution.process.CapturingProcessHandler
//import com.intellij.execution.process.ProcessOutput
//import com.intellij.openapi.components.service
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.ProgressManager
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.registry.Registry
//import com.intellij.openapi.util.registry.RegistryValue
//import com.intellij.util.net.HttpConfigurable
//import java.nio.file.Path
//
//val Project.cangjieSettings: CangJieProjectSettingsService
//    get() = service<CangJieProjectSettingsService>()
//
//
//class Cjc(toolchain: CjToolchainBase) : CangJieupComponent(NAME, toolchain) {
//    companion object {
//        const val NAME = "cjc"
//    }
//}
//
//
//abstract class CangJieupComponent(componentName: String, toolchain: CjToolchainBase) : CjTool(componentName, toolchain)
//abstract class CjTool(toolName: String, val toolchain: CjToolchainBase) {
//    open val executable: Path = toolchain.pathToExecutable(toolName)
//
//    protected open fun createBaseCommandLine(
//        parameters: List<String>,
//        workingDirectory: Path? = null,
//        environment: Map<String, String> = emptyMap()
//    ): GeneralCommandLine = GeneralCommandLine(executable)
//        .withWorkDirectory(workingDirectory)
//        .withParameters(parameters)
//        .withEnvironment(environment)
//        .withCharset(Charsets.UTF_8)
//        .also { toolchain.patchCommandLine(it) }
//
//    protected fun createBaseCommandLine(
//        vararg parameters: String,
//        workingDirectory: Path? = null,
//        environment: Map<String, String> = emptyMap()
//    ): GeneralCommandLine = createBaseCommandLine(
//        parameters.toList(),
//        workingDirectory = workingDirectory,
//        environment = environment
//    )
//}
//
//class Cjpm(
//    toolchain: CjToolchainBase,
//    useWrapper: Boolean = false
//) : CangJieupComponent(if (useWrapper) WRAPPER_NAME else NAME, toolchain) {
//    private var _http: HttpConfigurable? = null
//    private val http: HttpConfigurable
//        get() = _http ?: HttpConfigurable.getInstance()
//
//    fun CjpmCommandLine.patchArgs(project: Project, colors: Boolean): CjpmCommandLine {
//        val (pre, post) = splitOnDoubleDash()
//            .let { (pre, post) -> pre.toMutableList() to post.toMutableList() }
//
//        if (command in listOf("test", "bench")) {
//            if (allFeatures && !pre.contains("--all-features")) {
//                pre.add("--all-features")
//            }
//            if (TEST_NOCAPTURE_ENABLED_KEY.asBoolean() && !post.contains("--nocapture")) {
//                post.add(0, "--nocapture")
//            }
//        }
//
//
//        // Force colors
//        val forceColors = colors &&
//                command in COLOR_ACCEPTING_COMMANDS &&
//                additionalArguments.none { it.startsWith("--color") }
//        if (forceColors) pre.add(0, "--color=always")
//
//        return copy(additionalArguments = if (post.isEmpty()) pre else pre + "--" + post)
//    }
//
//
//    fun checkSupportForBuildCheckAllTargets(): Boolean {
//        val lines = createBaseCommandLine("help", "check")
//            .execute(toolchain.executionTimeoutInMilliseconds)
//            ?.stdoutLines
//            ?: return false
//        return lines.any { it.contains(" --all-targets ") }
//    }
//
//    private fun toGeneralCommandLine(
//        project: Project,
//        commandLine: CjpmCommandLine,
//        colors: Boolean
//    ): GeneralCommandLine =
//        with(commandLine.patchArgs(project, colors)) {
//            val parameters = buildList {
//                when {
//
//                    toolchain != null -> add("+$toolchain")
//                }
//
//                add(command)
//                addAll(additionalArguments)
//            }
//            val rustcExecutable = this@Cjpm.toolchain.cjc().executable.toString()
//            this@Cjpm.toolchain.createGeneralCommandLine(
//                executable,
//                workingDirectory,
//                redirectInputFrom,
//
//                environmentVariables,
//                parameters,
//                http = http
//
//            ).withEnvironment("CJC", rustcExecutable)
//        }
//
//    fun toColoredCommandLine(project: Project, commandLine: CjpmCommandLine): GeneralCommandLine =
//        toGeneralCommandLine(project, commandLine, colors = true)
//
//    companion object {
//        const val NAME = "cjpm"
//        const val WRAPPER_NAME = "cjpm"
//        private val FEATURES_ACCEPTING_COMMANDS: List<String> = listOf(
//            "bench",
//            "build",
//            "check",
//            "doc",
//            "fix",
//            "run",
//            "rustc",
//            "rustdoc",
//            "test",
//            "metadata",
//            "tree",
//            "install",
//            "package",
//            "publish"
//        )
//
//        val LOG = Logger.getInstance(Cjpm::class.java)
//
//        @JvmStatic
//        val TEST_NOCAPTURE_ENABLED_KEY: RegistryValue = Registry.get("com.huawei.cangjie.cjpm.test.nocapture")
//        private val COLOR_ACCEPTING_COMMANDS: List<String> = listOf(
//            "bench", "build", "check", "clean", "clippy", "doc", "install", "publish", "run", "rustc", "test", "update"
//        )
//    }
//}
//
//fun CapturingProcessHandler.runProcess(
//    indicator: ProgressIndicator?,
//    timeoutInMilliseconds: Int? = null
//): ProcessOutput {
//    return when {
//        indicator != null && timeoutInMilliseconds != null ->
//            runProcessWithProgressIndicator(indicator, timeoutInMilliseconds)
//
//        indicator != null -> runProcessWithProgressIndicator(indicator)
//        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
//        else -> runProcess()
//    }
//}
//
//private fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
//    return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)
//}
//fun GeneralCommandLine.execute(timeoutInMilliseconds: Int?): ProcessOutput? {
//
//    val handler = CjCapturingProcessHandler.startProcess(this).unwrapOrElse {
//        LOG.warn("Failed to run executable", it)
//        return null
//    }
//    val output = handler.runProcessWithGlobalProgress(timeoutInMilliseconds)
//
//    if (!output.isSuccess) {
//        LOG.warn(CjProcessExecutionException.errorMessage(commandLineString, output))
//    }
//
//    return output
//}
//val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0
