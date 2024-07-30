package com.huawei.cangjie.ide.run.cjpm

import com.huawei.cangjie.cjpm.CjpmCommands
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.ide.project.settings.ui.fullWidthCell
import com.huawei.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.huawei.cangjie.ide.run.CjCommandConfiguration
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.impl.SingleConfigurationConfigurable
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.externalSystem.service.execution.cmd.ParametersListLexer
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.ExpandableEditorSupport
import com.intellij.ui.TextAccessor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Function
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.nullize
import com.intellij.util.textCompletion.TextFieldWithCompletion
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JPanel


class CjpmCommandConfigurationEditor(project: Project) :
    CjCommandConfigurationEditor<CjpmCommandConfiguration>(project) {

    private var panel: JComponent? = null


    override val command =
        CjCommandLineEditor(project, CjpmCommandCompletionProvider(project.cjpmProjects) { currentWorkspace() })


    override fun createEditor(): JComponent = panel {
        row(CangJieUiBundle.message("action.run.cjpm.configuration.command.title")) {
            fullWidthCell(command)
                .component


        }
//
//        row(workingDirectory.label) {
//            fullWidthCell(workingDirectory)
//                .resizableColumn()
//
//        }
    }.also { panel = it }

    private val buildOnRemoteTarget = CheckBox(CangJieUiBundle.message("checkbox.build.on.remote.target"), true)
    private val isRemoteTarget: Boolean
        get() = DataManager.getInstance().getDataContext(panel)
            .getData(SingleConfigurationConfigurable.RUN_ON_TARGET_NAME_KEY) != null

    private fun hideUnsupportedFieldsIfNeeded() {
        if (!ApplicationManager.getApplication().isDispatchThread) return
        buildOnRemoteTarget.isVisible = isRemoteTarget
    }

    override fun resetEditorFrom(configuration: CjpmCommandConfiguration) {
        super.resetEditorFrom(configuration)

        hideUnsupportedFieldsIfNeeded()

    }

    override fun applyEditorTo(configuration: CjpmCommandConfiguration) {
        super.applyEditorTo(configuration)


        hideUnsupportedFieldsIfNeeded()
    }
}


abstract class CjCommandConfigurationEditor<T : CjCommandConfiguration>(
    protected val project: Project
) : SettingsEditor<T>() {
    abstract val command: CjCommandLineEditor
    protected val currentWorkingDirectory: Path?
        get() = workingDirectory.component.text.nullize()?.let { Paths.get(it) }
    protected val workingDirectory: LabeledComponent<TextFieldWithBrowseButton> =
        WorkingDirectoryComponent()

    protected fun currentWorkspace(): CjpmWorkspace? =
        CjpmCommandConfiguration.findCjpmProject(project, command.text, currentWorkingDirectory)?.workspace

    override fun resetEditorFrom(configuration: T) {
        command.text = configuration.command
    }

    override fun applyEditorTo(configuration: T) {
        configuration.command = command.text
    }

}

class CjCommandLineEditor(
    private val project: Project,
    private val completionProvider: TextFieldCompletionProvider
) : JPanel(BorderLayout()), TextAccessor {
    private val textField = createTextField("")
    private fun createTextField(value: String): TextFieldWithCompletion =
        TextFieldWithCompletion(
            project,
            completionProvider,
            value,
            true,
            false,
            false
        )

    init {
        ExpandableEditorSupportWithCustomPopup(textField, this::createTextField)
        add(textField, BorderLayout.CENTER)
    }

    override fun setText(text: String) {

        textField.text = text

    }

    override fun getText(): String = textField.text

}

private fun getCompleterForOption(name: String): ArgCompleter? = when (name) {
//    "bin" -> targetCompleter(CjpmWorkspace.TargetKind.Bin)
//    "example" -> targetCompleter(CjpmWorkspace.TargetKind.ExampleBin)
//    "test" -> targetCompleter(CjpmWorkspace.TargetKind.Test)
//    "bench" -> targetCompleter(CjpmWorkspace.TargetKind.Bench)
//    "package" -> { ctx -> ctx.currentWorkspace?.packages.orEmpty().map { it.lookupElement } }
//    "manifest-path" -> { ctx -> ctx.projects.map { it.lookupElement } }
//    "target" -> { ctx -> getTargetTripleCompletions(ctx) }
    else -> null
}

class CjpmCommandCompletionProvider(
    projects: CjpmProjectsService,
    implicitTextPrefix: String,
    workspaceGetter: () -> CjpmWorkspace?
) : CjCommandCompletionProvider(projects, implicitTextPrefix, workspaceGetter) {


    constructor(projects: CjpmProjectsService, workspaceGetter: () -> CjpmWorkspace?)
            : this(projects, "", workspaceGetter)

    constructor(projects: CjpmProjectsService, workspace: CjpmWorkspace?) : this(projects, { workspace })

    override val commonCommands: List<CmdBase> = buildList {
        for (command in CjpmCommands.entries) {
            Cmd(command.presentableName) {
                for (option in command.options) {
                    val completer = getCompleterForOption(option.name)
                    if (completer != null) {
                        opt(option.name, completer)
                    } else {
                        flag(option.name)
                    }
                }
            }.also { add(it) }
        }
    }

    private class Cmd(name: String, initOptions: CjpmOptBuilder.() -> Unit = {}) : CmdBase(name) {
        override val options: List<Opt> = CjpmOptBuilder().apply(initOptions).result
    }

    private class CjpmOptBuilder(override val result: MutableList<Opt> = mutableListOf()) : OptBuilder

}

interface OptBuilder {
    val result: MutableList<Opt>

    fun flag(long: String) {
        result += Opt(long)
    }

    fun opt(long: String, argCompleter: ArgCompleter) {
        result += Opt(long, argCompleter)
    }
}


data class Context(
    val projects: Collection<CjpmProject>,
    val currentWorkspace: CjpmWorkspace?,
    val commandLinePrefix: List<String>
)
typealias ArgCompleter = (Context) -> List<LookupElement>

class Opt(
    val name: String,
    val argCompleter: ArgCompleter? = null
) {
    val long get() = "--$name"

    val lookupElement: LookupElement =
        LookupElementBuilder.create(long)
            .withInsertHandler { ctx, _ ->
                if (argCompleter != null) {
                    ctx.addSuffix(" ")
                    ctx.setLaterRunnable {
                        CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(ctx.project, ctx.editor)
                    }
                }
            }
}

fun InsertionContext.addSuffix(suffix: String) {
    document.insertString(selectionEndOffset, suffix)
    EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
}

abstract class CmdBase(
    val name: String
) {
    abstract val options: List<Opt>
    val lookupElement: LookupElement =
        LookupElementBuilder.create(name).withInsertHandler { ctx, _ ->
            ctx.addSuffix(" ")
        }
}

abstract class CjCommandCompletionProvider(
    val projects: CjpmProjectsService,
    private val implicitTextPrefix: String,
    val workspaceGetter: () -> CjpmWorkspace?
) : TextFieldCompletionProvider() {

    fun splitContextPrefix(text: String): Pair<String, String> {
        val lexer = ParametersListLexer(text)
        var contextEnd = 0
        while (lexer.nextToken()) {
            if (lexer.tokenEnd == text.length) {
                return text.substring(0, contextEnd) to lexer.currentToken
            }
            contextEnd = lexer.tokenEnd
        }

        return text.substring(0, contextEnd) to ""
    }

    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        val (ctx, _) = splitContextPrefix(text)
        result.addAllElements(complete(ctx))
    }

    protected abstract val commonCommands: List<CmdBase>

    fun complete(context: String): List<LookupElement> {
        val args = ParametersListUtil.parse(implicitTextPrefix + context)
        if ("--" in args) return emptyList()
        if (args.isEmpty()) {
            return commonCommands.map { it.lookupElement }
        }

        val cmd = commonCommands.find { it.name == args.firstOrNull() } ?: return emptyList()

        val argCompleter = cmd.options.find { it.long == args.lastOrNull() }?.argCompleter
        if (argCompleter != null) {
            return argCompleter(Context(projects.allProjects, workspaceGetter(), args))
        }

        return cmd.options
            .filter { it.long !in args }
            .map { it.lookupElement }
    }

}

private class ExpandableEditorSupportWithCustomPopup(
    field: EditorTextField,
    private val createPopup: (text: String) -> EditorTextField
) : ExpandableEditorSupport(field) {

    override fun createPopupEditor(field: EditorTextField, text: String): EditorTextField {
        return createPopup(text)
    }

//        @Suppress("UnstableApiUsage")
//    override fun prepare(field: EditorTextField, onShow: Function<in String, String>): Content {
//            val popup = createPopup(onShow.`fun`(field.text))
//        val background = field.background
//
//        popup.background = background
//        popup.setOneLineMode(false)
//        popup.preferredSize = Dimension(field.width, 5 * field.height)
//        popup.addSettingsProvider { editor ->
//            initPopupEditor(editor, background)
//            copyCaretPosition(editor, field.editor)
//        }
//
//        return object : Content {
//            override fun getContentComponent(): JComponent = popup
//            override fun getFocusableComponent(): JComponent = popup
//            override fun cancel(onHide: Function<in String, String>) {
//                field.text = onHide.`fun`(popup.text)
//                val editor = field.editor
//                if (editor != null) copyCaretPosition(editor, popup.editor)
//                if (editor is EditorEx) updateFieldFolding((editor as EditorEx?)!!)
//            }
//        }
//    }
//
//    companion object {
//        private fun copyCaretPosition(destination: Editor, source: Editor?) {
//            if (source == null) return  // unexpected
//            try {
//                destination.caretModel.caretsAndSelections = source.caretModel.caretsAndSelections
//            } catch (ignored: IllegalArgumentException) {
//            }
//        }
//    }
}

private class WorkingDirectoryComponent : LabeledComponent<TextFieldWithBrowseButton>() {
    init {
        component = TextFieldWithBrowseButton().apply {
            val fileChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                title = ExecutionBundle.message("select.working.directory.message")
            }
            addBrowseFolderListener(null, null, null, fileChooser)
        }
        text = ExecutionBundle.message("run.configuration.working.directory.label")
    }
}
