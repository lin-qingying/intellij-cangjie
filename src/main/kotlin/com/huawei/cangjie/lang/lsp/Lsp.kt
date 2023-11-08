package com.huawei.cangjie.lang.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.services.WorkspaceService

import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.LanguageServer
import java.io.PipedOutputStream

import java.io.PipedInputStream
import java.io.File
import java.security.DrbgParameters.Capability
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


//class  CangJieLanguageClent : IntellijLanguageClient() {
//
//
//}


class CangJieLanguageClent : LanguageClient {
    /**
     * 处理遥测事件
     */
    override fun telemetryEvent(`object`: Any?) {
        println(`object`)
    }

    /**
     * 处理诊断信息
     */
    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {
        println(diagnostics)
    }

    /**
     * 显示消息
     */
    override fun showMessage(messageParams: MessageParams?) {
        println(messageParams)
    }

    /**
     * 显示消息请求，并返回一个Future表示用户的响应
     */
    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> {
        return CompletableFuture.completedFuture(MessageActionItem("Response"))
    }

    /**
     * 日志消息
     */
    override fun logMessage(message: MessageParams?) {
        println(message)
    }


}


fun main() {


    val cangJieLanguageClent = CangJieLanguageClent()
    val dir = File("D:\\Code\\idea\\intellij-cangjie\\lsp")
//
    val processBuilder = ProcessBuilder("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe", "--src")
    processBuilder.directory(dir)
    //启动lsp服务进程
    val process = processBuilder.start()

//
//
//    //获取lsp服务进程的输入流
    val inputStream = process.inputStream
//    //获取lsp服务进程的输出流
    val outputStream = process.outputStream
//
//
//

//

//
//    创建并启动Launcher
    val launcher = LSPLauncher.createClientLauncher(cangJieLanguageClent, inputStream, outputStream)


    //获取语言服务
    val server = launcher.remoteProxy
    launcher.startListening()

    //创建初始化参数
    val initParams = InitializeParams()
    initParams.processId = ProcessHandle.current().pid().toInt()



    initParams.rootUri = "file:///D%3A/Code/idea/intellij-cangjie/testData"
    initParams.rootPath = "D:\\Code\\idea\\intellij-cangjie\\testData"







    initParams.capabilities = getCapabilities()
//initParams.initializationOptions = getInitializationOptions()
    initParams.trace = "off"
    //设置工作区文件夹
    val workspaceFolder = WorkspaceFolder()
    workspaceFolder.name = "testData"
    workspaceFolder.uri = "D:\\Code\\idea\\intellij-cangjie\\testData"
    initParams.workspaceFolders = listOf(workspaceFolder)
    initParams.clientInfo = ClientInfo("Intellij Idea CangJie", "1.0.0")
    initParams.locale = "zh_cn"

    val res = server.initialize(initParams).get()
    println(res)

//    2023-11-04 11:43:01 1699069381204:[Info]receive message body:{"jsonrpc":"2.0","method":"initialized","params":{}}
//    2023-11-04 11:43:01 1699069381208:[Info]receive message body:{"jsonrpc":"2.0","method":"textDocument/didOpen","params":{"textDocument":{"uri":"file:///d%3A/Code/Cj/xml/src/main.cj","languageId":"Cangjie","version":1,"text":" \nclass a{\n    private prop b :Int {\n        get() {\n            return 1\n        }\n   \n     }\n}\n\nmain() {\n    \n}"}}}

    server.initialized(InitializedParams())

    val textDocumentService = server.textDocumentService
    val textDocumentItem = TextDocumentItem()
    textDocumentItem.uri = "file:///d%3A/Code/Cj/xml/src/main.cj"
    textDocumentItem.languageId = "Cangjie"
    textDocumentItem.version = 1
    textDocumentItem.text =
        " \nclass a{\n    private prop b :Int {\n        get() {\n            return 1\n        }\n   \n     }\n}\n\nmain() {\n    \n}"
    textDocumentService.didOpen(DidOpenTextDocumentParams(textDocumentItem))


}

fun getInitializationOptions() {
//        "initializationOptions": {
//      "multiModuleOption": {
//        "file:///d%3A/Code/Cj/xml": {
//          "name": "xml",
//          "package_requires": {
//            "path_option": [],
//            "package_option": {}
//          },
//          "requires": {}
//        }
//      },
//      "conditionCompileOption": {},
//      "singleConditionCompileOption": {}
//    },


}


fun getCapabilities(): ClientCapabilities {
    val capabilities = ClientCapabilities()
    val workspace = WorkspaceClientCapabilities()
//    //{"jsonrpc":"2.0","id":0,"method":"initialize","params":{"processId":22140,"clientInfo":{"name":"Visual Studio Code","version":"1.84.0"},"locale":"zh-cn","rootPath":"d:\\Code\\Cj\\xml","rootUri":"file:///d%3A/Code/Cj/xml","capabilities":{"workspace":{"applyEdit":true,"workspaceEdit":{"documentChanges":true,"resourceOperations":["create","rename","delete"],"failureHandling":"textOnlyTransactional","normalizesLineEndings":true,"changeAnnotationSupport":{"groupsOnLabel":true}},"configuration":true,"didChangeWatchedFiles":{"dynamicRegistration":true,"relativePatternSupport":true},"symbol":{"dynamicRegistration":true,"symbolKind":{"valueSet":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26]},"tagSupport":{"valueSet":[1]},"resolveSupport":{"properties":["location.range"]}},"codeLens":{"refreshSupport":true},"executeCommand":{"dynamicRegistration":true},"didChangeConfiguration":{"dynamicRegistration":true},"workspaceFolders":true,"semanticTokens":{"refreshSupport":true},"fileOperations":{"dynamicRegistration":true,"didCreate":true,"didRename":true,"didDelete":true,"willCreate":true,"willRename":true,"willDelete":true},"inlineValue":{"refreshSupport":true},"inlayHint":{"refreshSupport":true},"diagnostics":{"refreshSupport":true}},"textDocument":{"publishDiagnostics":{"relatedInformation":true,"versionSupport":false,"tagSupport":{"valueSet":[1,2]},"codeDescriptionSupport":true,"dataSupport":true},"synchronization":{"dynamicRegistration":true,"willSave":true,"willSaveWaitUntil":true,"didSave":true},"completion":{"dynamicRegistration":true,"contextSupport":true,"completionItem":{"snippetSupport":true,"commitCharactersSupport":true,"documentationFormat":["markdown","plaintext"],"deprecatedSupport":true,"preselectSupport":true,"tagSupport":{"valueSet":[1]},"insertReplaceSupport":true,"resolveSupport":{"properties":["documentation","detail","additionalTextEdits"]},"insertTextModeSupport":{"valueSet":[1,2]},"labelDetailsSupport":true},"insertTextMode":2,"completionItemKind":{"valueSet":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25]},"completionList":{"itemDefaults":["commitCharacters","editRange","insertTextFormat","insertTextMode"]},"editsNearCursor":true},"hover":{"dynamicRegistration":true,"contentFormat":["markdown","plaintext"]},"signatureHelp":{"dynamicRegistration":true,"signatureInformation":{"documentationFormat":["markdown","plaintext"],"parameterInformation":{"labelOffsetSupport":true},"activeParameterSupport":true},"contextSupport":true},"definition":{"dynamicRegistration":true,"linkSupport":true},"references":{"dynamicRegistration":true},"documentHighlight":{"dynamicRegistration":true},"documentSymbol":{"dynamicRegistration":true,"symbolKind":{"valueSet":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26]},"hierarchicalDocumentSymbolSupport":true,"tagSupport":{"valueSet":[1]},"labelSupport":true},"codeAction":{"dynamicRegistration":true,"isPreferredSupport":true,"disabledSupport":true,"dataSupport":true,"resolveSupport":{"properties":["edit"]},"codeActionLiteralSupport":{"codeActionKind":{"valueSet":["","quickfix","refactor","refactor.extract","refactor.inline","refactor.rewrite","source","source.organizeImports"]}},"honorsChangeAnnotations":false},"codeLens":{"dynamicRegistration":true},"formatting":{"dynamicRegistration":true},"rangeFormatting":{"dynamicRegistration":true},"onTypeFormatting":{"dynamicRegistration":true},"rename":{"dynamicRegistration":true,"prepareSupport":true,"prepareSupportDefaultBehavior":1,"honorsChangeAnnotations":true},"documentLink":{"dynamicRegistration":true,"tooltipSupport":true},"typeDefinition":{"dynamicRegistration":true,"linkSupport":true},"implementation":{"dynamicRegistration":true,"linkSupport":true},"colorProvider":{"dynamicRegistration":true},"foldingRange":{"dynamicRegistration":true,"rangeLimit":5000,"lineFoldingOnly":true,"foldingRangeKind":{"valueSet":["comment","imports","region"]},"foldingRange":{"collapsedText":false}},"declaration":{"dynamicRegistration":true,"linkSupport":true},"selectionRange":{"dynamicRegistration":true},"callHierarchy":{"dynamicRegistration":true},"semanticTokens":{"dynamicRegistration":true,"tokenTypes":["namespace","type","class","enum","interface","struct","typeParameter","parameter","variable","property","enumMember","event","function","method","macro","keyword","modifier","comment","string","number","regexp","operator","decorator"],"tokenModifiers":["declaration","definition","readonly","static","deprecated","abstract","async","modification","documentation","defaultLibrary"],"formats":["relative"],"requests":{"range":true,"full":{"delta":true}},"multilineTokenSupport":false,"overlappingTokenSupport":false,"serverCancelSupport":true,"augmentsSyntaxTokens":true},"linkedEditingRange":{"dynamicRegistration":true},"typeHierarchy":{"dynamicRegistration":true},"inlineValue":{"dynamicRegistration":true},"inlayHint":{"dynamicRegistration":true,"resolveSupport":{"properties":["tooltip","textEdits","label.tooltip","label.location","label.command"]}},"diagnostic":{"dynamicRegistration":true,"relatedDocumentSupport":false}},"window":{"showMessage":{"messageActionItem":{"additionalPropertiesSupport":true}},"showDocument":{"support":true},"workDoneProgress":true},"general":{"staleRequestSupport":{"cancel":true,"retryOnContentModified":["textDocument/semanticTokens/full","textDocument/semanticTokens/range","textDocument/semanticTokens/full/delta"]},"regularExpressions":{"engine":"ECMAScript","version":"ES2020"},"markdown":{"parser":"marked","version":"1.1.0"},"positionEncodings":["utf-16"]},"notebookDocument":{"synchronization":{"dynamicRegistration":true,"executionSummarySupport":true}}},"initializationOptions":{"multiModuleOption":{"file:///d%3A/Code/Cj/xml":{"name":"xml","package_requires":{"path_option":[],"package_option":{}},"requires":{}}},"conditionCompileOption":{},"singleConditionCompileOption":{}},"trace":"off","workspaceFolders":[{"uri":"file:///d%3A/Code/Cj/xml","name":"xml"}],"workDoneToken":"f59f2995-4c7d-4cc0-971b-581edb616683"}}
    workspace.applyEdit = true
    val workspaceEdit = WorkspaceEdit()
//    workspaceEdit.documentChanges = Either.forLeft(listOf(TextDocumentEdit()))
//    workspaceEdit.resourceOperations =
//        listOf(ResourceOperationKind.Create, ResourceOperationKind.Rename, ResourceOperationKind.Delete)
//    workspaceEdit.failureHandling = FailureHandlingKind.TextOnlyTransactional
//    workspaceEdit.normalizesLineEndings = true
//    val changeAnnotationSupport = ChangeAnnotationSupport()
//    changeAnnotationSupport.groupsOnLabel = true
//    workspaceEdit.changeAnnotationSupport = changeAnnotationSupport
//    workspace.workspaceEdit = workspaceEdit
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
    val documentationFormat   = listOf("markdown", "plaintext")
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
    val tokenTypes =   listOf(
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
    val staleRequestSupportCapabilities = StaleRequestCapabilities(true,listOf(
        "textDocument/semanticTokens/full",
        "textDocument/semanticTokens/range",
        "textDocument/semanticTokens/full/delta"
    ))

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
