package com.linqingying.cangjie.ide.formatter.cjfmt

import com.linqingying.cangjie.cjpm.toolchain.cjfmt
import com.linqingying.cangjie.ide.run.cjpm.runconfig.ExecuteResult
import com.linqingying.cangjie.ide.run.cjpm.toolchain
import com.intellij.codeInsight.actions.LastRunReformatCodeOptionsProvider
import com.intellij.codeInsight.actions.ReformatCodeRunOptions
import com.intellij.codeInsight.actions.TextRangeType
import com.intellij.codeInsight.actions.VcsFacade
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.SmartList
import java.util.*
import java.util.function.Consumer

enum class ErrorInfo(val value: String) {
    SDK_ERROR("Please check Cangjie SDK path.")
}

@JvmRecord
data class LineRange(val startLine: Int, val endLine: Int) {


    companion object {
        fun create(startLine: Int, endLine: Int): LineRange {
            return LineRange(startLine, endLine)
        }
    }
}

@Service(Service.Level.PROJECT)
class CangJieFormatService {

    companion object {

        fun getInstance(project: Project): CangJieFormatService {
            return project.getService(CangJieFormatService::class.java)

        }

        private val LOG = Logger.getInstance(CangJieFormatService::class.java)
        private const val FORMAT_FILE = "-f"
        private const val FORMAT_LINE = "-l"
    }

    fun reformatDirectoryCode(file: VirtualFile, project: Project, formatOption: String): Optional<ExecuteResult> {
        return this.getExecuteResult(file, project, formatOption)
    }

    private fun getExecuteResult(file: VirtualFile, project: Project, formatOption: String): Optional<ExecuteResult> {


        val cjfmt = project.toolchain?.cjfmt() ?: return Optional.of<ExecuteResult>(
            ExecuteResult(
                1,
                ErrorInfo.SDK_ERROR.value
            )
        )
        return cjfmt.executeCommand(formatOption, FileUtil.toSystemIndependentName(file.path))


    }


    fun reformatCode(file: VirtualFile, project: Project): Optional<ExecuteResult> {
        if (!file.exists()) {
            LOG.warn("File does not exist: " + file.path)
            return Optional.empty<ExecuteResult>()
        } else {
            return if (file.isDirectory) this.reformatDirectoryCode(
                file,
                project,
                "-d"
            ) else this.getExecuteResult(file, project, "-f")
        }
    }

    private fun reformatSelectedCode(file: VirtualFile, editor: Editor, project: Project): Optional<ExecuteResult> {

        val cjfmt = project.toolchain?.cjfmt()
        if (cjfmt == null) {
            LOG.warn(
                "Reformat Selected Code error, file path is ${file.path}",
                )
            return Optional.of<ExecuteResult>(

                ExecuteResult(
                    1,
                    ErrorInfo.SDK_ERROR.value
                )
            )
        }
        val ranges: Collection<LineRange> = this.getRangesToFormat(editor)
        ranges.forEach(Consumer { lineRange: LineRange ->


            val line = "" + lineRange.startLine + ":" + lineRange.endLine
            val executeResult: Optional<ExecuteResult> =
                cjfmt.executeCommand("-f", FileUtil.toSystemIndependentName(file.path), "-l", line)
            if (executeResult.isEmpty) {
                LOG.warn(
                    "Reformat Selected Code error, file path is ${file.path}",

                    )
            }
        })
        return Optional.of(ExecuteResult(0, ""))

    }

    private fun getRangesToFormat(editor: Editor): Collection<LineRange> {

        val ranges: MutableList<LineRange> = SmartList()
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection()) {
            val startLine = editor.document.getLineNumber(selectionModel.selectionStart)
            val endLine = editor.document.getLineNumber(selectionModel.selectionEnd)
            val lineRange: LineRange = LineRange.create(startLine + 1, endLine + 1)
            ranges.add(lineRange)
        }



        return ranges
    }

    private fun getProcessingScope(
        currentRunOptions: ReformatCodeRunOptions,
        hasSelection: Boolean,
        project: Project,
        file: PsiFile
    ): TextRangeType {
        var processingScope = currentRunOptions.textRangeType
        if (hasSelection) {
            processingScope = TextRangeType.SELECTED_TEXT
        } else if (processingScope == TextRangeType.VCS_CHANGED_TEXT) {
            if (VcsFacade.getInstance().isChangeNotTrackedForFile(project, file)) {
                processingScope = TextRangeType.WHOLE_FILE
            }
        } else {
            processingScope = TextRangeType.WHOLE_FILE
        }

        return processingScope
    }

    private fun getProcessSelectedText(file: PsiFile, hasSelection: Boolean, project: Project): Boolean {
        val provider = LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance())
        val currentRunOptions = provider.getLastRunOptions(file)
        val processingScope: TextRangeType =
            this.getProcessingScope(currentRunOptions, hasSelection, project, file)
        currentRunOptions.setProcessingScope(processingScope)
        return currentRunOptions.textRangeType == TextRangeType.SELECTED_TEXT
    }

    fun reformatFileCode(file: PsiFile, hasSelection: Boolean, project: Project, editor: Editor) {
        val isProcessSelectedText: Boolean = this.getProcessSelectedText(file, hasSelection, project)
        val resultOptional = if (isProcessSelectedText) {
            this.reformatSelectedCode(file.virtualFile, editor, project)
        } else {
            this.reformatCode(file.virtualFile, project)
        }

        if (resultOptional.isEmpty) {
            notifyResult(false, project)
        } else {
            if (resultOptional.get().exitCode !== 0) {
                showWithProject(
                    resultOptional.get().executeOut,
                    "Reformat Code",
                    Type.INFO,
                    project
                )
            }
        }
    }
}
