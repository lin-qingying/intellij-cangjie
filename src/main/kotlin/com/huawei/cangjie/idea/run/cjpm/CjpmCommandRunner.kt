package com.huawei.cangjie.idea.run.cjpm

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjRunConfigurationExtensionManager
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.huawei.cangjie.idea.run.cjpm.runconfig.startProcess
import com.huawei.cangjie.idea.run.hasRemoteTarget
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.RunTargetsEnabled
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentsManager
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.UserDataHolderBase



open class CjpmCommandRunner :CjDefaultProgramRunnerBase() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        if (executorId != DefaultRunExecutor.EXECUTOR_ID || profile !is CjpmCommandConfiguration) return false
        val cleaned = profile.clean().ok ?: return false
        val isLocalRun = !profile.hasRemoteTarget || profile.buildTarget.isRemote
        val isLegacyTestRun = !profile.isBuildToolWindowAvailable &&
                cleaned.cmd.command.command in listOf("test", "bench") &&
                getBuildConfiguration(profile) != null
        return isLocalRun && !isLegacyTestRun
    }


    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val configuration = environment.runProfile
        return if (configuration is CjpmCommandConfiguration &&
            !(isBuildConfiguration(configuration) && configuration.isBuildToolWindowAvailable)) {
            super.doExecute(state, environment)
        } else {
            // For commands like `cargo build` or `cargo test --no-run`
            // we skip execution here because build already was performed
            // in Build Tool window
            environment.putUserData(ExecutionManagerImpl.EXECUTION_SKIP_RUN, true)
            null
        }
    }

    override fun getRunnerId(): String =  RUNNER_ID

    companion object {

        val RUNNER_ID: String = "CjpmCommandRunner"

    }
}
typealias FeatureName = String
typealias PathConverter = (String) -> String

typealias PackageId = String

sealed class CompilerMessage {
    abstract val package_id: PackageId

    abstract fun convertPaths(converter: PathConverter): CompilerMessage

    companion object {
        fun fromJson(json: JsonObject): CompilerMessage? {
            val reason = json.getAsJsonPrimitive("reason")?.asString ?: return null
            val cls: Class<out CompilerMessage> = when (reason) {
                BuildScriptMessage.REASON -> BuildScriptMessage::class.java
                CompilerArtifactMessage.REASON -> CompilerArtifactMessage::class.java
                else -> return null
            }
            return Gson().fromJson(json, cls)
        }
    }
}

data class BuildScriptMessage(
    override val package_id: PackageId,
    val cfgs: List<String>,
    val env: List<List<String>>,
    val out_dir: String?
) : CompilerMessage() {

    override fun convertPaths(converter: PathConverter): BuildScriptMessage = copy(
        out_dir = out_dir?.let(converter)
    )

    companion object {
        const val REASON: String = "build-script-executed"
    }
}

data class Profile(
    val test: Boolean
)

data class CompilerArtifactMessage(
    override val package_id: PackageId,
    val target: CjpmMetadata.Target,
    val profile: Profile,
    val filenames: List<String>,
    val executable: String?
) : CompilerMessage() {

    val executables: List<String>
        get() {
            return if (executable != null) {
                listOf(executable)
            } else {
                /**
                 * `.dSYM` and `.pdb` files are binaries, but they should not be used when starting debug session.
                 * Without this filtering, CLion shows error message about several binaries
                 * in case of disabled build tool window
                 */
                // BACKCOMPAT: Cargo 0.34.0
                filenames.filter { !it.endsWith(".dSYM") && !it.endsWith(".pdb") }
            }
        }

    override fun convertPaths(converter: PathConverter): CompilerArtifactMessage = copy(
        target = target.convertPaths(converter),
        filenames = filenames.map(converter),
        executable = executable?.let(converter)
    )

    companion object {
        const val REASON: String = "compiler-artifact"

        fun fromJson(json: JsonObject): CompilerArtifactMessage? {
            if (json.getAsJsonPrimitive("reason").asString != REASON) {
                return null
            }
            return Gson().fromJson(json, CompilerArtifactMessage::class.java)
        }
    }
}

fun CjpmRunStateBase.executeCommandLine(
    commandLine: GeneralCommandLine,
    environment: ExecutionEnvironment
): DefaultExecutionResult {
    val runConfiguration = configuration
    val targetEnvironment = runConfiguration.targetEnvironment
    val context = ConfigurationExtensionContext()

    val extensionManager = CjRunConfigurationExtensionManager.getInstance()
    extensionManager.patchCommandLine(runConfiguration, environment, commandLine, context)
    extensionManager.patchCommandLineState(runConfiguration, environment, this, context)
    val handler =
        commandLine.startProcess(environment.project, targetEnvironment, processColors = true, uploadExecutable = true)
    extensionManager.attachExtensionsToProcess(runConfiguration, handler, environment, context)

    val console = consoleBuilder.console
    handler.let { console.attachToProcess(it) }
    return DefaultExecutionResult(console, handler)

}

val CjpmCommandConfiguration.targetEnvironment: TargetEnvironmentConfiguration?
    get() {
        if (!RunTargetsEnabled.get()) return null
        val targetName = defaultTargetName ?: return null
        return TargetEnvironmentsManager.getInstance(project).targets.findByName(targetName)
    }

class ConfigurationExtensionContext : UserDataHolderBase()
object CjpmMetadata {
    data class Project(

        val packages: List<Package>,

        val resolve: Resolve,

        val version: Int,

        val workspace_members: List<String>,


        val workspace_root: String
    ) {
        fun convertPaths(converter: PathConverter): Project = copy(
            packages = packages.map { it.convertPaths(converter) },
            workspace_root = converter(workspace_root)
        )
    }

    data class Package(
        val name: String,


        val version: String,

        val authors: List<String>,

        val description: String?,

        val repository: String?,

        val license: String?,

        val license_file: String?,


        val source: String?,


        val id: PackageId,

        val manifest_path: String,


        val targets: List<Target>,


        val edition: String?,


        val features: Map<FeatureName, List<FeatureDep>>,


        val dependencies: List<RawDependency>
    ) {
        fun convertPaths(converter: PathConverter): Package = copy(
            manifest_path = converter(manifest_path),
            targets = targets.map { it.convertPaths(converter) }
        )
    }

    data class RawDependency(

        val name: String,

        val rename: String?,
        val kind: String?,
        val target: String?,
        val optional: Boolean,
        val uses_default_features: Boolean,
        val features: List<String>
    )


    data class Target(

        val kind: List<String>,


        val name: String,


        val src_path: String,


        val crate_types: List<String>,


        val edition: String?,


        val doctest: Boolean?,

        @Suppress("KDocUnresolvedReference")
        @JsonProperty("required-features")
        val required_features: List<String>?
    ) {
        val cleanKind: TargetKind
            get() = when (kind.singleOrNull()) {
                "bin" -> TargetKind.BIN
                "example" -> TargetKind.EXAMPLE
                "test" -> TargetKind.TEST
                "bench" -> TargetKind.BENCH
                "proc-macro" -> TargetKind.LIB
                "custom-build" -> TargetKind.CUSTOM_BUILD
                else ->
                    if (kind.any { it.endsWith("lib") })
                        TargetKind.LIB
                    else
                        TargetKind.UNKNOWN
            }

        val cleanCrateTypes: List<CrateType>
            get() = crate_types.map {
                when (it) {
                    "bin" -> CrateType.BIN
                    "lib" -> CrateType.LIB
                    "dylib" -> CrateType.DYLIB
                    "staticlib" -> CrateType.STATICLIB
                    "cdylib" -> CrateType.CDYLIB
                    "rlib" -> CrateType.RLIB
                    "proc-macro" -> CrateType.PROC_MACRO
                    else -> CrateType.UNKNOWN
                }
            }

        fun convertPaths(converter: PathConverter): Target = copy(
            src_path = converter(src_path)
        )
    }

    enum class TargetKind {
        LIB, BIN, TEST, EXAMPLE, BENCH, CUSTOM_BUILD, UNKNOWN
    }


    enum class CrateType {
        BIN, LIB, DYLIB, STATICLIB, CDYLIB, RLIB, PROC_MACRO, UNKNOWN
    }

    data class Resolve(
        val nodes: List<ResolveNode>
    )


    data class ResolveNode(
        val id: PackageId,


        val dependencies: List<PackageId>,


        val deps: List<Dep>?,

        val features: List<String>?
    )

    data class Dep(

        val pkg: PackageId,


        val name: String?,


        @Suppress("KDocUnresolvedReference")
        val dep_kinds: List<DepKindInfo>?
    )

    data class DepKindInfo(
        val kind: String?,
        val target: String?
    )


    private val DYNAMIC_LIBRARY_EXTENSIONS: List<String> = listOf(".dll", ".so", ".dylib")


    fun Project.replacePaths(replacer: (String) -> String): Project =
        copy(
            packages = packages.map { it.replacePaths(replacer) },
            workspace_root = replacer(workspace_root)
        )

    private fun Package.replacePaths(replacer: (String) -> String): Package =
        copy(
            manifest_path = replacer(manifest_path),
            targets = targets.map { it.replacePaths(replacer) }
        )

    private fun Target.replacePaths(replacer: (String) -> String): Target =
        copy(src_path = replacer(src_path))
}

private val LOG: Logger = logger<CjpmMetadata>()
typealias FeatureDep = String
