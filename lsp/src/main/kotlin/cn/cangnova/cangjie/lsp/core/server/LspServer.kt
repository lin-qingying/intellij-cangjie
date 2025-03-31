package cn.cangnova.cangjie.lsp.core.server

import cn.cangnova.cangjie.lang.lsp.CangJieLspServerManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ReflectionUtil
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.io.URLUtil
import com.intellij.util.io.systemIndependentPath

import org.eclipse.lsp4j.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.cjpm.project.model.currentCjpmProject
import cn.cangnova.cangjie.cjpm.project.settings.cangjieSettings
import cn.cangnova.cangjie.cjpm.project.workspace.PackageOrigin
import com.intellij.openapi.application.ApplicationManager
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageServer
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

open class LspServerProcessListener(private val processHandler: OSProcessHandler) : ProcessListener {

    private val outputStreamWriter: OutputStreamWriter
    val pipedInputStream: PipedInputStream


    init {
        try {
            val pipedOutputStream = PipedOutputStream()
            outputStreamWriter = OutputStreamWriter(pipedOutputStream, StandardCharsets.UTF_8)
            pipedInputStream = PipedInputStream(pipedOutputStream)
        } catch (e: IOException) {
            throw ExecutionException(e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(
            LspServerProcessListener::class.java
        )

    }

    override fun startNotified(event: ProcessEvent) {

        LOG.info("LSP server process started: $processHandler")
    }

    override fun processTerminated(event: ProcessEvent) {


        LOG.info("LSP server process terminated, exit code = " + event.exitCode + ", command line: " + processHandler)
        try {
            outputStreamWriter.close()
            pipedInputStream.close()
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (ProcessOutputType.isStdout(outputType)) {
            val text = event.text

            try {
                outputStreamWriter.write(text)
                outputStreamWriter.flush()
            } catch (exception: IOException) {
                LOG.error(
                    "Problem proxying data to the listener, stopping process: ${processHandler.process}; " +
                            ReflectionUtil.dumpFields(
                                PipedInputStream::class.java,
                                pipedInputStream,
                                *arrayOf("readSide", "writeSide", "closedByReader", "closedByWriter")

                            ),
                    exception
                )
                ExecutionManagerImpl.stopProcess(processHandler)
            }
        } else if (ProcessOutputType.isStderr(outputType)) {
            LOG.info("${processHandler}\n STDERR: ${event.text}")
        }
    }


}

abstract class LspServerDescriptor(
    val project: Project,
    @NlsSafe val presentableName: String,
    vararg val roots: VirtualFile
) {
    open fun getFilePath(file: VirtualFile) = file.path
    open fun getFileUri(file: VirtualFile): String {
        val escapedPath = URLUtil.encodePath(getFilePath(file))
        val url = VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, escapedPath)
        val uri = VfsUtil.toUri(url)
        return uri?.toString() ?: url
    }

    open val clientCapabilities: ClientCapabilities = ClientCapabilities().apply {
        workspace = WorkspaceClientCapabilities().apply {
            workspaceFolders = true
            //configuration = true // keep false by default because [getWorkspaceConfiguration] returns null by default
            workspaceEdit = WorkspaceEditCapabilities().apply {
                documentChanges = true
                failureHandling = FailureHandlingKind.Abort
            }
        }
        textDocument = TextDocumentClientCapabilities().apply {
            definition = DefinitionCapabilities().apply {
                linkSupport = true
            }
            completion = CompletionCapabilities().apply {
                completionItem = CompletionItemCapabilities().apply {
                    documentationFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                    deprecatedSupport = true
                    tagSupport = CompletionItemTagSupportCapabilities(listOf(CompletionItemTag.Deprecated))
                    insertReplaceSupport = true
                    labelDetailsSupport = true
                }
                completionList = CompletionListCapabilities().apply {
                    itemDefaults = listOf("commitCharacters", "editRange", "insertTextFormat", "insertTextMode", "data")
                }
            }
            hover = HoverCapabilities().apply {
                contentFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
            }
            publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                versionSupport = true
                tagSupport =
                    Either.forRight(DiagnosticsTagSupport(listOf(DiagnosticTag.Unnecessary, DiagnosticTag.Deprecated)))
            }
            codeAction = CodeActionCapabilities().apply {
                codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
                    codeActionKind = CodeActionKindCapabilities(listOf(CodeActionKind.QuickFix))
                }
                disabledSupport = true
            }
        }
        notebookDocument = null
        window = WindowClientCapabilities().apply {
            showMessage = WindowShowMessageRequestCapabilities()
        }
        general = null
        experimental = null
    }

    open fun createInitializationOptions(): Any? = null

    open fun createInitializeParams(): InitializeParams {
        return InitializeParams().apply {
            capabilities = clientCapabilities
            if (roots.size == 1) {
                // Some old servers might need this old way of setting roots
                @Suppress("DEPRECATION")
                rootUri = getFileUri(roots[0])
                @Suppress("DEPRECATION")
                rootPath = getFilePath(roots[0])
            }
            workspaceFolders = roots.map { root: VirtualFile -> WorkspaceFolder(getFileUri(root), root.name) }
            createInitializationOptions()?.let { initializationOptions = it }
        }
    }
}

// 定义一个枚举类，表示LSP服务器的状态
enum class LspServerStatus {
    // 运行中
    RUNNING,

    // 已停止
    STOPPED,

    // 正在启动
    STARTING,

    // 正在停止
    STOPPING,

    // 正在重启
    RESTARTING,

    // 已重启
    RESTARTED,

    // 错误
    ERROR,

    // 未知
    UNKNOWN
}

interface LspServer {

    val project: Project

    val status: LspServerStatus
    fun isRunning(): Boolean = status == LspServerStatus.RUNNING
    val lsp4jServer: LanguageServer
    val lsp4jServerClass: Class<out LanguageServer> get() = LanguageServer::class.java
    val descriptor: LspServerDescriptor

    fun createInitializeParams(): InitializeParams {
        return descriptor.createInitializeParams()
    }

    val serverOutputStream: OutputStream
    val serverInputStream: InputStream

    fun connect(success: () -> Unit)
    fun start()
    fun stop()

}

internal class LspProcessHandler(generalCommandLine: GeneralCommandLine) : OSProcessHandler(generalCommandLine) {
    override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forMostlySilentProcess()
    }


}

class LspServerDescriptorImpl(project: Project, @NlsSafe presentableName: String) :
    LspServerDescriptor(project, presentableName, *project.getBaseDirectories().toTypedArray()) {

    fun getCapabilities(): ClientCapabilities {
        val capabilities = ClientCapabilities()
        val workspace = WorkspaceClientCapabilities()
        workspace.applyEdit = true


//        "workspaceEdit": {
//            "documentChanges": true,
//            "resourceOperations": [
//            "create",
//            "rename",
//            "delete"
//            ],
//            "failureHandling": "textOnlyTransactional",
//            "normalizesLineEndings": true,
//            "changeAnnotationSupport": {
//            "groupsOnLabel": true
//        }
//        },

        val workedit = WorkspaceEditCapabilities(

        ).apply {
            documentChanges = true
            resourceOperations = listOf(

                "create",

                "rename",

                "delete"

            )
            failureHandling = "textOnlyTransactional"

            normalizesLineEndings = true

            changeAnnotationSupport = WorkspaceEditChangeAnnotationSupportCapabilities().apply {
                groupsOnLabel = true
            }
        }

        workspace.workspaceEdit = workedit
        workspace.configuration = true
        val didChangeWatchedFiles = DidChangeWatchedFilesCapabilities()
        didChangeWatchedFiles.dynamicRegistration = true
        didChangeWatchedFiles.relativePatternSupport = true
        workspace.didChangeWatchedFiles = didChangeWatchedFiles
        val symbolCapabilities = SymbolCapabilities()
        symbolCapabilities.dynamicRegistration = true
        val symbolKind = SymbolKindCapabilities()
        symbolKind.valueSet = listOf(
            SymbolKind.File,
            SymbolKind.Module,
            SymbolKind.Namespace,
            SymbolKind.Package,
            SymbolKind.Class,
            SymbolKind.Method,
            SymbolKind.Property,
            SymbolKind.Field,
            SymbolKind.Constructor,
            SymbolKind.Enum,
            SymbolKind.Interface,
            SymbolKind.Function,
            SymbolKind.Variable,
            SymbolKind.Constant,
            SymbolKind.String,
            SymbolKind.Number,
            SymbolKind.Boolean,
            SymbolKind.Array,
            SymbolKind.Object,
            SymbolKind.Key,
            SymbolKind.Null,
            SymbolKind.EnumMember,
            SymbolKind.Struct,
            SymbolKind.Event,
            SymbolKind.Operator,
            SymbolKind.TypeParameter
        )
        symbolCapabilities.symbolKind = symbolKind
//    val tagSupport = SymbolTagSupportCapabilities()
//    tagSupport.valueSet = listOf(SymbolTag.DynamicRegistration)
//    symbolCapabilities.tagSupport = tagSupport
        val resolveSupport = WorkspaceSymbolResolveSupportCapabilities()
        resolveSupport.properties = listOf("location.range")
        symbolCapabilities.resolveSupport = resolveSupport
        workspace.symbol = symbolCapabilities
        val codeLensCapabilities = CodeLensWorkspaceCapabilities(true)

        workspace.codeLens = codeLensCapabilities
        val executeCommandCapabilities = ExecuteCommandCapabilities()
        executeCommandCapabilities.dynamicRegistration = true
        workspace.executeCommand = executeCommandCapabilities
        val didChangeConfigurationCapabilities = DidChangeConfigurationCapabilities()
        didChangeConfigurationCapabilities.dynamicRegistration = true
        workspace.didChangeConfiguration = didChangeConfigurationCapabilities
        workspace.workspaceFolders = true
        val semanticTokensCapabilities = SemanticTokensWorkspaceCapabilities(true)

        workspace.semanticTokens = semanticTokensCapabilities
        val fileOperationsCapabilities = FileOperationsWorkspaceCapabilities()
        fileOperationsCapabilities.dynamicRegistration = true
        fileOperationsCapabilities.didCreate = true
        fileOperationsCapabilities.didRename = true
        fileOperationsCapabilities.didDelete = true
        fileOperationsCapabilities.willCreate = true
        fileOperationsCapabilities.willRename = true
        fileOperationsCapabilities.willDelete = true
        workspace.fileOperations = fileOperationsCapabilities
        val inlineValueCapabilities = InlineValueWorkspaceCapabilities(true)

        workspace.inlineValue = inlineValueCapabilities


        val diagnosticsCapabilities = DiagnosticWorkspaceCapabilities()
        diagnosticsCapabilities.refreshSupport = true
        workspace.diagnostics = diagnosticsCapabilities
        capabilities.workspace = workspace
        val textDocument = TextDocumentClientCapabilities()
        val publishDiagnosticsCapabilities = PublishDiagnosticsCapabilities()
        publishDiagnosticsCapabilities.relatedInformation = true
        publishDiagnosticsCapabilities.versionSupport = false


        textDocument.rename = RenameCapabilities().apply {
            dynamicRegistration = true
            prepareSupport = true
            prepareSupportDefaultBehavior = PrepareSupportDefaultBehavior.Identifier
            honorsChangeAnnotations = true
        }

        publishDiagnosticsCapabilities.codeDescriptionSupport = true
        publishDiagnosticsCapabilities.dataSupport = true
        textDocument.publishDiagnostics = publishDiagnosticsCapabilities
        val synchronizationCapabilities = SynchronizationCapabilities()
        synchronizationCapabilities.dynamicRegistration = true
        synchronizationCapabilities.willSave = true
        synchronizationCapabilities.willSaveWaitUntil = true
        synchronizationCapabilities.didSave = true
        textDocument.synchronization = synchronizationCapabilities
        val completionCapabilities = CompletionCapabilities()
        completionCapabilities.dynamicRegistration = true
        completionCapabilities.contextSupport = true
        val completionItemCapabilities = CompletionItemCapabilities()
        completionItemCapabilities.snippetSupport = true
        completionItemCapabilities.commitCharactersSupport = true
//    completionItemCapabilities.documentationFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText)
        completionItemCapabilities.deprecatedSupport = true
        completionItemCapabilities.preselectSupport = true
        val tagSupport2 = CompletionItemTagSupportCapabilities()
        tagSupport2.valueSet = listOf(CompletionItemTag.Deprecated)
        completionItemCapabilities.tagSupport = tagSupport2
        completionItemCapabilities.insertReplaceSupport = true
        val resolveSupport1 = CompletionItemResolveSupportCapabilities()
        resolveSupport1.properties = listOf("documentation", "detail", "additionalTextEdits")
        completionItemCapabilities.resolveSupport = resolveSupport1
        val insertTextModeSupport = CompletionItemInsertTextModeSupportCapabilities()
//    insertTextModeSupport.valueSet = listOf(InsertTextMode.Insert, InsertTextMode.AdjustIndentation)
        completionItemCapabilities.insertTextModeSupport = insertTextModeSupport
        completionItemCapabilities.labelDetailsSupport = true
        completionCapabilities.completionItem = completionItemCapabilities
        val completionListCapabilities = CompletionListCapabilities()
        completionListCapabilities.itemDefaults =
            listOf("commitCharacters", "editRange", "insertTextFormat", "insertTextMode")
        completionCapabilities.completionList = completionListCapabilities

        textDocument.completion = completionCapabilities
        val hoverCapabilities = HoverCapabilities()
        hoverCapabilities.dynamicRegistration = true
//    val contentFormat = ContentFormatCapabilities()
//    contentFormat.valueSet = listOf(MarkupKind.Markdown, MarkupKind.PlainText)
//    hoverCapabilities.contentFormat = contentFormat
        textDocument.hover = hoverCapabilities
        val signatureHelpCapabilities = SignatureHelpCapabilities()
        signatureHelpCapabilities.dynamicRegistration = true
        val signatureInformationCapabilities = SignatureInformationCapabilities()
        val documentationFormat = listOf("markdown", "plaintext")
        signatureInformationCapabilities.documentationFormat = documentationFormat
        val parameterInformationCapabilities = ParameterInformationCapabilities()
        parameterInformationCapabilities.labelOffsetSupport = true
        signatureInformationCapabilities.parameterInformation = parameterInformationCapabilities
        signatureInformationCapabilities.activeParameterSupport = true
        signatureHelpCapabilities.signatureInformation = signatureInformationCapabilities
        signatureHelpCapabilities.contextSupport = true
        textDocument.signatureHelp = signatureHelpCapabilities
        val definitionCapabilities = DefinitionCapabilities()
        definitionCapabilities.dynamicRegistration = true
        definitionCapabilities.linkSupport = true
        textDocument.definition = definitionCapabilities
        val referencesCapabilities = ReferencesCapabilities()
        referencesCapabilities.dynamicRegistration = true
        textDocument.references = referencesCapabilities
        val documentHighlightCapabilities = DocumentHighlightCapabilities()
        documentHighlightCapabilities.dynamicRegistration = true
        textDocument.documentHighlight = documentHighlightCapabilities
        val documentSymbolCapabilities = DocumentSymbolCapabilities()
        documentSymbolCapabilities.dynamicRegistration = true
        val symbolKind1 = SymbolKindCapabilities()
        symbolKind1.valueSet = listOf(
            SymbolKind.File,
            SymbolKind.Module,
            SymbolKind.Namespace,
            SymbolKind.Package,
            SymbolKind.Class,
            SymbolKind.Method,
            SymbolKind.Property,
            SymbolKind.Field,
            SymbolKind.Constructor,
            SymbolKind.Enum,
            SymbolKind.Interface,
            SymbolKind.Function,
            SymbolKind.Variable,
            SymbolKind.Constant,
            SymbolKind.String,
            SymbolKind.Number,
            SymbolKind.Boolean,
            SymbolKind.Array,
            SymbolKind.Object,
            SymbolKind.Key,
            SymbolKind.Null,
            SymbolKind.EnumMember,
            SymbolKind.Struct,
            SymbolKind.Event,
            SymbolKind.Operator,
            SymbolKind.TypeParameter
        )
        documentSymbolCapabilities.symbolKind = symbolKind1
        val tagSupport3 = SymbolTagSupportCapabilities()
        tagSupport3.valueSet = listOf(SymbolTag.Deprecated)
        documentSymbolCapabilities.tagSupport = tagSupport3
        documentSymbolCapabilities.labelSupport = true
        textDocument.documentSymbol = documentSymbolCapabilities
        val codeActionCapabilities = CodeActionCapabilities()
        codeActionCapabilities.dynamicRegistration = true
        codeActionCapabilities.isPreferredSupport = true
        codeActionCapabilities.disabledSupport = true
        codeActionCapabilities.dataSupport = true
        val resolveSupport2 = CodeActionResolveSupportCapabilities()
        resolveSupport2.properties = listOf("edit")
        codeActionCapabilities.resolveSupport = resolveSupport2
        val codeActionLiteralSupport = CodeActionLiteralSupportCapabilities()
        val codeActionKindCapabilities = CodeActionKindCapabilities()
        codeActionKindCapabilities.valueSet = listOf(
            "",
            CodeActionKind.QuickFix,
            CodeActionKind.Refactor,
            CodeActionKind.RefactorExtract,
            CodeActionKind.RefactorInline,
            CodeActionKind.RefactorRewrite,
            CodeActionKind.Source,
            CodeActionKind.SourceOrganizeImports
        )
        codeActionLiteralSupport.codeActionKind = codeActionKindCapabilities
        codeActionCapabilities.codeActionLiteralSupport = codeActionLiteralSupport
        codeActionCapabilities.honorsChangeAnnotations = false
        textDocument.codeAction = codeActionCapabilities
        val codeLensCapabilities1 = CodeLensCapabilities()
        codeLensCapabilities1.dynamicRegistration = true
        textDocument.codeLens = codeLensCapabilities1
        val formattingCapabilities = FormattingCapabilities()
        formattingCapabilities.dynamicRegistration = true
        textDocument.formatting = formattingCapabilities
        val rangeFormattingCapabilities = RangeFormattingCapabilities()
        rangeFormattingCapabilities.dynamicRegistration = true
        textDocument.rangeFormatting = rangeFormattingCapabilities
        val onTypeFormattingCapabilities = OnTypeFormattingCapabilities()
        onTypeFormattingCapabilities.dynamicRegistration = true
        textDocument.onTypeFormatting = onTypeFormattingCapabilities
//    val renameCapabilities = RenameCapabilities()
//    renameCapabilities.dynamicRegistration = true
//    renameCapabilities.prepareSupport = true
//    renameCapabilities.prepareSupportDefaultBehavior = PrepareSupportDefaultBehavior.Preview
//    renameCapabilities.honorsChangeAnnotations = true
//    textDocument.rename = renameCapabilities
        val documentLinkCapabilities = DocumentLinkCapabilities()
        documentLinkCapabilities.dynamicRegistration = true
        documentLinkCapabilities.tooltipSupport = true
        textDocument.documentLink = documentLinkCapabilities
        val typeDefinitionCapabilities = TypeDefinitionCapabilities()
        typeDefinitionCapabilities.dynamicRegistration = true
        typeDefinitionCapabilities.linkSupport = true
        textDocument.typeDefinition = typeDefinitionCapabilities
        val implementationCapabilities = ImplementationCapabilities()
        implementationCapabilities.dynamicRegistration = true
        implementationCapabilities.linkSupport = true
        textDocument.implementation = implementationCapabilities
        val colorProviderCapabilities = ColorProviderCapabilities()
        colorProviderCapabilities.dynamicRegistration = true
        textDocument.colorProvider = colorProviderCapabilities
        val foldingRangeCapabilities = FoldingRangeCapabilities()
        foldingRangeCapabilities.dynamicRegistration = true
        foldingRangeCapabilities.rangeLimit = 5000
        foldingRangeCapabilities.lineFoldingOnly = true
        val foldingRangeKindCapabilities = FoldingRangeKindSupportCapabilities()
        foldingRangeKindCapabilities.valueSet =
            listOf(FoldingRangeKind.Comment, FoldingRangeKind.Imports, FoldingRangeKind.Region)
        foldingRangeCapabilities.foldingRangeKind = foldingRangeKindCapabilities
        val foldingRangeCapabilities1 = FoldingRangeSupportCapabilities()
        foldingRangeCapabilities1.collapsedText = false
        foldingRangeCapabilities.foldingRange = foldingRangeCapabilities1
        textDocument.foldingRange = foldingRangeCapabilities
        val declarationCapabilities = DeclarationCapabilities()
        declarationCapabilities.dynamicRegistration = true
        declarationCapabilities.linkSupport = true
        textDocument.declaration = declarationCapabilities
        val selectionRangeCapabilities = SelectionRangeCapabilities()
        selectionRangeCapabilities.dynamicRegistration = true
        textDocument.selectionRange = selectionRangeCapabilities
        val callHierarchyCapabilities = CallHierarchyCapabilities()
        callHierarchyCapabilities.dynamicRegistration = true
        textDocument.callHierarchy = callHierarchyCapabilities
        val semanticTokensCapabilities1 = SemanticTokensCapabilities()
        semanticTokensCapabilities1.dynamicRegistration = true
        val tokenTypes = listOf(
            "namespace",
            "type",
            "class",
            "enum",
            "interface",
            "struct",
            "typeParameter",
            "parameter",
            "variable",
            "property",
            "enumMember",
            "event",
            "function",
            "method",
            "macro",
            "keyword",
            "modifier",
            "comment",
            "string",
            "number",
            "regexp",
            "operator",
            "decorator"
        )
        semanticTokensCapabilities1.tokenTypes = tokenTypes
        val tokenModifiers = listOf(
            "declaration",
            "definition",
            "readonly",
            "static",
            "deprecated",
            "abstract",
            "async",
            "modification",
            "documentation",
            "defaultLibrary"
        )
        semanticTokensCapabilities1.tokenModifiers = tokenModifiers
//    val formats = SemanticTokenFormatsCapabilities()
//    formats.valueSet = listOf("relative")
//    semanticTokensCapabilities1.formats = formats
        val requests = SemanticTokensClientCapabilitiesRequests()
//    requests.range = true
//    val full = Either<Boolean, SemanticTokensClientCapabilitiesRequestsFull>(true)
//    full.delta = true
//    requests.full = full
        semanticTokensCapabilities1.requests = requests
        semanticTokensCapabilities1.multilineTokenSupport = false
        semanticTokensCapabilities1.overlappingTokenSupport = false
        semanticTokensCapabilities1.serverCancelSupport = true
        semanticTokensCapabilities1.augmentsSyntaxTokens = true
        textDocument.semanticTokens = semanticTokensCapabilities1
        val linkedEditingRangeCapabilities = LinkedEditingRangeCapabilities()
        linkedEditingRangeCapabilities.dynamicRegistration = true
        textDocument.linkedEditingRange = linkedEditingRangeCapabilities
        val typeHierarchyCapabilities = TypeHierarchyCapabilities()
        typeHierarchyCapabilities.dynamicRegistration = true
        textDocument.typeHierarchy = typeHierarchyCapabilities
        val inlineValueCapabilities1 = InlineValueCapabilities()
        inlineValueCapabilities1.dynamicRegistration = true
        textDocument.inlineValue = inlineValueCapabilities1
//    val inlayHintCapabilities1 = InlayHintCapabilities()
//    inlayHintCapabilities1.dynamicRegistration = true
//    val resolveSupport3 = InlayHintResolveSupportCapabilities()
//    resolveSupport3.properties = listOf("tooltip", "textEdits", "label.tooltip", "label.location", "label.command")
//    inlayHintCapabilities1.resolveSupport = resolveSupport3
//    textDocument.inlayHints = inlayHintCapabilities1
        val diagnosticCapabilities = DiagnosticCapabilities()
        diagnosticCapabilities.dynamicRegistration = true
        diagnosticCapabilities.relatedDocumentSupport = false
        textDocument.diagnostic = diagnosticCapabilities
        capabilities.textDocument = textDocument
        val window = WindowClientCapabilities()
        val showMessageCapabilities = WindowShowMessageRequestCapabilities()
        val messageActionItemCapabilities = WindowShowMessageRequestActionItemCapabilities()
        messageActionItemCapabilities.additionalPropertiesSupport = true
        showMessageCapabilities.messageActionItem = messageActionItemCapabilities
        window.showMessage = showMessageCapabilities
        val showDocumentCapabilities = ShowDocumentCapabilities(true)
//    showDocumentCapabilities.support = true
        window.showDocument = showDocumentCapabilities
        window.workDoneProgress = true
        capabilities.window = window
        val general = GeneralClientCapabilities()
        val staleRequestSupportCapabilities = StaleRequestCapabilities(
            true, listOf(
                "textDocument/semanticTokens/full",
                "textDocument/semanticTokens/range",
                "textDocument/semanticTokens/full/delta"
            )
        )

        general.staleRequestSupport = staleRequestSupportCapabilities
        val regularExpressionsCapabilities = RegularExpressionsCapabilities()
        regularExpressionsCapabilities.engine = "ECMAScript"
        regularExpressionsCapabilities.version = "ES2020"
        general.regularExpressions = regularExpressionsCapabilities
        val markdownCapabilities = MarkdownCapabilities()
        markdownCapabilities.parser = "marked"
        markdownCapabilities.version = "1.1.0"
        general.markdown = markdownCapabilities
//    val positionEncodings = PositionEncodingsCapabilities()
//    positionEncodings.valueSet = listOf("utf-16")
//    general.positionEncodings = positionEncodings
        capabilities.general = general
        val notebookDocument = NotebookDocumentClientCapabilities()
        val synchronizationCapabilities1 = NotebookDocumentSyncClientCapabilities()
        synchronizationCapabilities1.dynamicRegistration = true

        notebookDocument.synchronization = synchronizationCapabilities1


        capabilities.notebookDocument = notebookDocument

        return capabilities

    }

    override fun createInitializeParams(): InitializeParams {

        val toolchain = project.cangjieSettings.toolchain


        val initializeParams = super.createInitializeParams()

        initializeParams.clientInfo = ClientInfo("Intellij CangJie", "1.0.0")

        initializeParams.processId = ProcessHandle.current().pid().toInt()
        initializeParams.capabilities = getCapabilities()
        initializeParams.locale = "zh_cn"
        initializeParams.trace = "off"


        if (toolchain != null) {

            var projectName = project.name

            fun getMap(): Map<String, Any> {
                return mapOf(
                    "modulesHomeOption" to toolchain.location.systemIndependentPath,
//                    "extensionPath" to "C:\\Users\\27439\\.cangjie\\lsp"
                    "telemetryOption" to true,

                    "multiModuleOption" to mutableMapOf<String, Any>(
                        getFileUri(project.guessProjectDir()!!) to mapOf(
                            "name" to projectName,

                            "package_requires" to mapOf(
                                "path_option" to listOf<String>(),
                                "package_option" to mapOf<String, String>()
                            ),
                            "requires" to mutableMapOf<String, Any>().apply {

                                if (project.cjpmProjects.currentCjpmProject?.isWorkspace == true) {
                                    if (project.currentCjpmProject?.workspace?.packages != null) {
                                        for (`package` in project.currentCjpmProject?.workspace?.packages!!) {

                                            if (`package`.origin == PackageOrigin.DEPENDENCY) {
                                                put(
                                                    `package`.name, mapOf(
                                                        "path" to `package`.contentRoot?.let { getFileUri(it) }
                                                    )
                                                )
                                            }

                                        }
                                    }

                                }
                            },
                        )
                    ).apply {
                        if (project.cjpmProjects.currentCjpmProject?.isWorkspace == true) {
                            if (project.currentCjpmProject?.workspace?.packages != null) {
                                for (`package` in project.currentCjpmProject?.workspace?.packages!!) {
//                                    `package`.contentRoot?.let { getFileUri(it) }?.let {
//                                        put(
//                                            it,
//                                            mapOf(
//                                                "name" to `package`.name,
//                                                "package_requires" to mapOf(
//                                                    "path_option" to listOf<String>(),
//                                                    "package_option" to mapOf<String, String>()
//                                                ),
//                                                "requires" to mutableMapOf<String, Any>().apply {
//
//                                                    for (module in `package`.moduleData?.requires!!) {
//                                                        put(
//                                                            module.name, mapOf(
//                                                                "path" to module.contentRoot(project)
//                                                                    ?.let { getFileUri(it) }
//                                                            )
//                                                        )
//                                                    }
//
//
////
//                                                }
//                                            )
//                                        )
//                                    }


                                    if (`package`.origin == PackageOrigin.DEPENDENCY) {
                                        `package`.contentRoot?.let { getFileUri(it) }?.let {
                                            put(
                                                it,
                                                mapOf(
                                                    "name" to `package`.name,
                                                    "package_requires" to mapOf(
                                                        "path_option" to listOf<String>(),
                                                        "package_option" to mapOf<String, String>()
                                                    ),
                                                    "requires" to mutableMapOf<String, Any>()
                                                )
                                            )
                                        }
                                    } else if (`package`.origin == PackageOrigin.WORKSPACE) {
                                        if (`package`.name != projectName) {
                                            projectName = `package`.name

                                            return getMap()
                                        }
                                    }

                                }
                            }

                        }
                    }
                )

            }


            val map = getMap()

            initializeParams.initializationOptions = map


        }

        return initializeParams

    }
}

class LspServerImpl(override val project: Project) : LspServer {
    companion object {
        val LOG = Logger.getInstance(LspServerImpl::class.java)
    }

    private var lsp4jServerConnector: Lsp4jServerConnector = Lsp4jServerConnector(this)
    private var state: AtomicReference<LspServerStatus> = AtomicReference(LspServerStatus.STOPPED)
    override val status: LspServerStatus
        get() = state.get()
    override val lsp4jServer: LanguageServer
        get() = lsp4jServerConnector.lsp4jServer ?: throw IllegalStateException("Server is not running")
    override val descriptor: LspServerDescriptor = LspServerDescriptorImpl(project, "CangJie LSP Server")


    override val serverOutputStream: OutputStream
        get() = processHandle.processInput
    override val serverInputStream: InputStream
        get() = processListener.pipedInputStream

    val processHandle: OSProcessHandler = LspProcessHandler(CangJieLspServerManager.getCommandLine(project))
    val processListener: LspServerProcessListener = object : LspServerProcessListener(processHandle) {
        override fun startNotified(event: ProcessEvent) {
            super.startNotified(event)
            connect{}



        }



        override fun processTerminated(event: ProcessEvent) {
            super.processTerminated(event)
            state.set(LspServerStatus.STOPPED)


        }
    }.apply {
        processHandle.addProcessListener(this)
    }


    private fun sendOpenedFiles() {
        TODO("Not yet implemented")
    }

    override fun connect(success: () -> Unit) {
        LOG.info("Starting server")
        openedFiles.clear()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                synchronized(connectorLock) {
                    lsp4jServerConnector.connect {
                        state.set(LspServerStatus.RUNNING)

                    }
                }
                success()
                sendOpenedFiles()
            } catch (e: Exception) {
                LOG.warn("Failed to start server", e)
//                        listenersAdapter.serverInitializationFailed()
                cleanupShutdownAndExit()
            }
        }
    }
    override fun start() {
        state.set(LspServerStatus.STARTING)

        processHandle.startNotify()
    }

    val openedFiles: MutableSet<VirtualFile> = Collections.synchronizedSet(HashSet())
    private val connectorLock: Any = Any()

    /**
     * 清理，关闭并退出
     */
    fun cleanupShutdownAndExit() {
        if (!state.compareAndSet(
                LspServerStatus.RUNNING,
                LspServerStatus.STOPPED
            )
        ) {
            LOG.debug("Attempt to stop server in wrong myState")
        } else {
            LOG.debug("Stopping server")
            openedFiles.clear()
//            requestExecutor.shutdownNow()
            val task = Runnable {
                synchronized(connectorLock) {
                    lsp4jServerConnector.shutdownExitDisconnect()
                }
            }
            if (!ApplicationManager.getApplication().isDispatchThread && !ApplicationManager.getApplication().isReadAccessAllowed) {
                task.run()
            } else {
                ApplicationManager.getApplication().executeOnPooledThread(task)
            }
        }
    }

    override fun stop() {
        state.set(LspServerStatus.STOPPING)
        processHandle.destroyProcess()

    }
}