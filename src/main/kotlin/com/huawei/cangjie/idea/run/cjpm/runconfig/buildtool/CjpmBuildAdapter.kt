package com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool


import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.project.CangJieProjectManager
import com.huawei.cangjie.idea.run.cjpm.CjpmConstants
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.actions.StopProcessAction
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.NonInjectable
import org.intellij.lang.annotations.Language
import java.nio.file.Paths
import javax.swing.JComponent
import kotlin.math.max


@Suppress("UnstableApiUsage")
class CjpmBuildAdapter(
    private val context: CjpmBuildContext,
    buildProgressListener: BuildProgressListener
) : CjpmBuildAdapterBase(context, buildProgressListener) {

    init {
        val processHandler = checkNotNull(context.processHandler) { "Process handler can't be null" }
        context.environment.notifyProcessStarted(processHandler)

        val buildContentDescriptor =
            BuildContentDescriptor(null, null, object : JComponent() {}, CangJieBundle.message("build"))
        val activateToolWindow = context.environment.isActivateToolWindowBeforeRun

        buildContentDescriptor.isActivateToolWindowWhenAdded = activateToolWindow
        buildContentDescriptor.isActivateToolWindowWhenFailed = activateToolWindow
//        buildContentDescriptor.isNavigateToError = context.project.rustSettings.autoShowErrorsInEditor

        val descriptor = DefaultBuildDescriptor(
            context.buildId,
            CangJieBundle.message("build.event.title.run.cjpm.command"),
            context.workingDirectory.toString(),
            context.started
        )
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(createRerunAction(processHandler, context.environment))
            .withRestartAction(createStopAction(processHandler))
            .apply { createFilters().forEach { withExecutionFilter(it) } }


        val buildStarted =
            StartBuildEventImpl(descriptor, CangJieBundle.message("build.event.message.running", context.taskName))
        buildProgressListener.onEvent(context.buildId, buildStarted)
    }


    override fun onBuildOutputReaderFinish(
        event: ProcessEvent,
        isSuccess: Boolean,
        isCanceled: Boolean,
        error: Throwable?
    ) {
        val (status, result) = when {
            isCanceled -> "canceled" to SkippedResultImpl()
            isSuccess -> "successful" to SuccessResultImpl()
            else -> "failed" to FailureResultImpl(error)
        }
        val buildFinished = FinishBuildEventImpl(
            context.buildId,
            null,
            System.currentTimeMillis(),
            CangJieBundle.message("build.event.message.", context.taskName, status),
            result
        )
        buildProgressListener.onEvent(context.buildId, buildFinished)
        context.finished(isSuccess)
        context.environment.notifyProcessTerminated(event.processHandler, event.exitCode)
        val targetPath = context.workingDirectory.resolve(CjpmConstants.ProjectLayout.target)
        val targetDir = VfsUtil.findFile(targetPath, true) ?: return
        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)

    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        context.environment.notifyProcessTerminating(event.processHandler)
    }

    companion object {

        private fun createStopAction(processHandler: ProcessHandler): StopProcessAction =
            StopProcessAction("Stop", "Stop", processHandler)


        private fun createRerunAction(
            processHandler: ProcessHandler,
            environment: ExecutionEnvironment
        ): RestartProcessAction =
            RestartProcessAction(processHandler, environment)

        private class RestartProcessAction(
            private val processHandler: ProcessHandler,
            private val environment: ExecutionEnvironment
        ) : DumbAwareAction() {
            override fun actionPerformed(e: AnActionEvent) {
                ExecutionManagerImpl.stopProcess(processHandler)
                ExecutionUtil.restart(environment)
            }

            private val isEnabled: Boolean
                get() {
                    val project = environment.project
                    val settings = environment.runnerAndConfigurationSettings
                    return (!DumbService.isDumb(project) || settings == null || settings.type.isDumbAware) &&
                            !ExecutorRegistry.getInstance().isStarting(environment) &&
                            !processHandler.isProcessTerminating
                }

            override fun update(event: AnActionEvent) {
                val presentation = event.presentation
                presentation.text =
                    CangJieBundle.message("action.rerun.text", StringUtil.escapeMnemonics(environment.runProfile.name))
                presentation.icon =
                    if (processHandler.isProcessTerminated) AllIcons.Actions.Compile else AllIcons.Actions.Restart
                presentation.isEnabled = isEnabled
            }
        }

    }

}

@Suppress("UnstableApiUsage")
abstract class CjpmBuildAdapterBase(
    private val context: CjpmBuildContextBase,
    protected val buildProgressListener: BuildProgressListener
) : ProcessAdapter() {
    private val instantReader = BuildOutputInstantReaderImpl(
        context.buildId,
        context.parentId,
        buildProgressListener,
        listOf(CjBuildEventsConverter(context))
    )

    override fun processTerminated(event: ProcessEvent) {
        instantReader.closeAndGetFuture().whenComplete { _, error ->
            val isSuccess = event.exitCode == 0 && context.errors.get() == 0
            val isCanceled = context.indicator?.isCanceled ?: false
            onBuildOutputReaderFinish(event, isSuccess = isSuccess, isCanceled = isCanceled, error)
        }
    }

    open fun onBuildOutputReaderFinish(
        event: ProcessEvent,
        isSuccess: Boolean,
        isCanceled: Boolean,
        error: Throwable?
    ) {
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        // Progress messages end with '\r' instead of '\n'. We want to replace '\r' with '\n'
        // so that `instantReader` sends progress messages to parsers separately from other messages.
        val text = StringUtil.convertLineSeparators(event.text)
        instantReader.append(text)
    }
}

fun createFilters(): Collection<Filter> = buildList {

    val dir = CangJieProjectManager.workspaceRootDir
    if (dir != null) {
        add(CjConsoleFilter(CangJieProjectManager.getCurrentProject(), dir))
        add(CjDbgFilter(CangJieProjectManager.getCurrentProject(), dir))
        add(CjPanicFilter(CangJieProjectManager.getCurrentProject(), dir))
//        add(CjBacktraceFilter(CangJieProjectManager.currentProject, dir, CangJieProjectManager.workspace))
    }
}

class CjDbgFilter(
    project: Project,
    cargoProjectDir: VirtualFile
) : RegexpFileLinkFilter(project, cargoProjectDir, "\\s*\\[$FILE_POSITION_RE].*")

class CjConsoleFilter(
    project: Project,
    cargoProjectDir: VirtualFile
) : RegexpFileLinkFilter(
    project,
    cargoProjectDir,
    "(?:\\s+--> )?${FILE_POSITION_RE}.*"
)

class CjPanicFilter(
    project: Project,
    cargoProjectDir: VirtualFile
) : RegexpFileLinkFilter(project, cargoProjectDir, "\\s*thread '.+' panicked at '.+', $FILE_POSITION_RE")

//
//class CjBacktraceFilter @NonInjectable constructor(
//    private val project: Project,
//    private val cargoProjectDir: VirtualFile?,
//
//) : Filter {
//    private val backtraceItemFilters: List<CjBacktraceItemFilter>
//        get() {
//            if (workspace == null) {
//                val filters = project.cargoProjects.allProjects
//                    .mapNotNull { it.workspace }
//                    .map { CjBacktraceItemFilter(project, it) }
//                if (filters.isNotEmpty()) return filters
//            }
//            return listOf(CjBacktraceItemFilter(project, workspace))
//        }
//
//    private val sourceLinkFilters: List<RegexpFileLinkFilter>
//        get() {
//            if (cargoProjectDir == null) {
//                return project.cargoProjects.allProjects
//                    .mapNotNull { it.rootDir }
//                    .map { RegexpFileLinkFilter(project, it, LINE_REGEX) }
//            }
//            return listOf(RegexpFileLinkFilter(project, cargoProjectDir, LINE_REGEX))
//        }
//
//
//    constructor(project: Project) : this(project, null, null)
//
//    override fun applyFilter(line: String, entireLength: Int): Filter.Result? =
//        (backtraceItemFilters.asSequence() + sourceLinkFilters.asSequence())
//            .mapNotNull { it.applyFilter(line, entireLength) }
//            .firstOrNull()
//
//    companion object {
//        val LINE_REGEX: String = "\\s+at $FILE_POSITION_RE"
//    }
//}


open class RegexpFileLinkFilter(
    private val project: Project,
    private val cargoProjectDirectory: VirtualFile,
    lineRegExp: String
) : Filter, DumbAware {

    companion object {
        // TODO: named groups when Kotlin supports them
        @Language("RegExp")
        val FILE_POSITION_RE = """((?:\p{Alpha}:)?[0-9 a-z_A-Z\-\\./]+):([0-9]+)(?::([0-9]+))?"""

        @Language("RegExp")
        private val RUSTC_ABSOLUTE_PATH_RE = Regex("""/rustc/\w+/(.*)""")
    }

    init {
        require(FILE_POSITION_RE in lineRegExp)
        require('^' !in lineRegExp && '$' !in lineRegExp)
    }

    private val linePattern = ("^$lineRegExp\\R?$").toRegex()

    // Line is a single sine, with line separator included
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val match = matchLine(line) ?: return null
        val fileGroup = match.groups[1]!!
        val lineNumber = match.groups[2]?.let { zeroBasedNumber(it.value) } ?: 0
        val columnNumber = match.groups[3]?.let { zeroBasedNumber(it.value) } ?: 0

        val lineStart = entireLength - line.length

        val file = resolveFilePath(fileGroup.value)
        val link = file?.let { OpenFileHyperlinkInfo(project, file.file, lineNumber, columnNumber) }

        val grayedOut = if (file == null) {
            false
        } else {
            file !is ResolvedPath.Workspace
        }

        val end = match.groups[3]?.range?.last
            ?: match.groups[2]?.range?.last
            ?: fileGroup.range.last
        return Filter.Result(
            lineStart + fileGroup.range.first,
            lineStart + end + 1,
            link,
            grayedOut
        )
    }

    fun matchLine(line: String): MatchResult? = linePattern.matchEntire(line)

    private fun zeroBasedNumber(number: String): Int {
        return try {
            max(0, number.toInt() - 1)
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun resolveFilePath(fileName: String): ResolvedPath? {
        val path = FileUtil.toSystemIndependentName(fileName)
        val file = cargoProjectDirectory.findFileByRelativePath(path)
        if (file != null) return ResolvedPath.Workspace(file)

        val externalPath = resolveStdlibPath(fileName) ?: resolveCargoPath(fileName)
        if (externalPath != null) return externalPath

        // try to resolve absolute path
        return cargoProjectDirectory.fileSystem.findFileByPath(path)?.let { ResolvedPath.Unknown(it) }
    }

    private fun resolveCargoPath(path: String): ResolvedPath? {
        if (!path.startsWith("/cargo")) return null
        val fullPath = Paths.get(getCjpmRoot(), path.removePrefix("/cargo")).toString()
        return cargoProjectDirectory.fileSystem.findFileByPath(fullPath)?.let { ResolvedPath.CargoDependency(it) }
    }

    private fun resolveStdlibPath(path: String): ResolvedPath? {
        val sysroot = getSysroot() ?: return null
        val normalizedPath = normalizeStdLibPath(path)
        val fullPath = "$sysroot/lib/rustlib/src/rust/$normalizedPath"
        return cargoProjectDirectory.fileSystem.findFileByPath(fullPath)?.let { ResolvedPath.Stdlib(it) }
    }

    // /rustc/<commit hash>/src/libstd/... -> src/libstd/...
    private fun normalizeStdLibPath(path: String): String {
        val match = RUSTC_ABSOLUTE_PATH_RE.matchEntire(path) ?: return path
        return match.groupValues[1]
    }

    private fun getSysroot(): String? = CangJieProjectManager.getCurrentProject().basePath
    private fun getCjpmRoot(): String = CangJieSdkManager.sdkPath

    sealed class ResolvedPath(val file: VirtualFile) {
        class Workspace(file: VirtualFile) : ResolvedPath(file)
        class Stdlib(file: VirtualFile) : ResolvedPath(file)
        class CargoDependency(file: VirtualFile) : ResolvedPath(file)
        class Unknown(file: VirtualFile) : ResolvedPath(file)
    }
}
