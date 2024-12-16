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

package com.linqingying.cangjie.lang.lsp



import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lang.lsWidget.LanguageServiceWidgetItem

import com.intellij.util.io.systemIndependentPath
import com.linqingying.cangjie.cjpm.project.model.Require
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.project.model.currentCjpmProject
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.cangjie.cjpm.project.workspace.PackageOrigin
import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CangJieIdeDescriptorOptions
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CangJieIdeDescriptorRenderer
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.lsp.CangJieLspServerManager.getCommandLine
//import com.linqingying.lsp.api.customization.LspFindReferencesSupport
//import com.linqingying.lsp.impl.documentation.DescriptionMarkup
//import com.linqingying.lsp.impl.documentation.LspDocumentationData
//import com.linqingying.lsp.impl.documentation.LspMarkDownFormat
import org.eclipse.lsp4j.*
//import com.intellij.platform.lsp.api.*
//
//import com.intellij.platform.lsp.api.customization.*
//import com.intellij.platform.lsp.api.lsWidget.*

import com.linqingying.lsp.api.*

import com.linqingying.lsp.api.customization.*
import com.linqingying.lsp.api.lsWidget.*


fun checkCangJieFIle(file: VirtualFile): Boolean {
//    return false
    if (file.extension == "cj") return true

    if (file.fileType is CangJieFileType) return true

    return false

}
//class CangJieLspMarkDownFormat : LspMarkDownFormat{
//    override fun formatMarkdown(markupContent: MarkupContent): LspDocumentationData {
//        val value = StringUtilRt.convertLineSeparators(markupContent.value)
//        val indexOfCodeBlock = value.indexOf("```", 3)
//
//        if (value.startsWith("```") && value.indexOf("\n") > 0 && indexOfCodeBlock >= 0) {
//            val trimmedHeader = value.takeWhile { !it.isWhitespace() }
//            val content = value.substring(3).trimStart()
//            val description = content.ifEmpty { null }
//            val codeBlock = value.substringAfter("\n", "").substringBefore("```").trimEnd()
//            val remaining = value.substring(indexOfCodeBlock + 3).trimStart()
//
//            return LspDocumentationData(codeBlock, description, remaining, DescriptionMarkup.MARKDOWN)
//        } else {
////       val descriptorRenderer = CangJieIdeDescriptorRenderer.withOptions { }
//            return LspDocumentationData(null, null, value, DescriptionMarkup.MARKDOWN)
//        }
//
//    }
//
//}

class CangJieLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (!CangJieLanguageServerServices.getInstance().lspConfig.enabled) return

        val cangjieSettings = project.cangjieSettings

        if (checkCangJieFIle(file)) {
            if (cangjieSettings.toolchain != null) {
                serverStarter.ensureServerStarted(CangJieLspServerDescriptor(project))
            }
        }

    }

    override fun createLspWidgetItems(project: Project, currentFile: VirtualFile?): List<LanguageServiceWidgetItem> {
        if (!CangJieLanguageServerServices.getInstance().lspConfig.enabled) return emptyList()

        return super.createLspWidgetItems(project, currentFile)
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem? {
        if (!CangJieLanguageServerServices.getInstance().lspConfig.enabled) return null
        return LspServerWidgetItem(lspServer, currentFile, CangJieIcons.CANGJIE)
    }

}


private class CangJieLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Cangjie") {
    override fun isSupportedFile(file: VirtualFile): Boolean {

        if (!CangJieLanguageServerServices.getInstance().lspConfig.enabled) return false
        return checkCangJieFIle(file)
//        如果未配置sdk


    }


    override fun getLanguageId(file: VirtualFile): String {
        return "Cangjie"
    }



    override fun createCommandLine(): GeneralCommandLine {
        return getCommandLine(project)
    }


    //  引用解析
    override val lspGoToDefinitionSupport
        get() = CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(
            Feature.GO_TO_DECLARATION
        )

    //代码补全
    override val lspCompletionSupport: LspCompletionSupport?
        get() {
            if (CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(Feature.AUTO_COMPLETE)) {
                return super.lspCompletionSupport
            }
            return null
        }

    //语义标记
    override val lspSemanticTokensSupport: LspSemanticTokensSupport?
        get() {
            if (CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(Feature.SEMANTIC_TOKENS))
                return LspSemanticTokensSupport()
            return null
        }

    //诊断信息
    override val lspDiagnosticsSupport: LspDiagnosticsSupport?
        get() {
            if (CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(Feature.DIAGNOSTICS))
                return super.lspDiagnosticsSupport
            return null
        }


    //查找用法
    override val lspFindReferencesSupport: LspFindReferencesSupport?
        get() {

            if (CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(Feature.FIND_USAGES)) {
                return super.lspFindReferencesSupport
            }
            return null
        }

    //    悬停
    override val lspHoverSupport: Boolean
        get() = CangJieLanguageServerServices.getInstance().lspConfig.isFeatureEnabled(Feature.HOVER_INFO)

//    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
//
//
//        println(handler)
//
//
//        return CangJieLsp4jClient(CangJieLspServerNotificationsHandler(handler))
//    }


    override fun createInitializeParams(): InitializeParams {

        val toolchain = project.cangjieSettings.toolchain


        val initializeParams = super.createInitializeParams()

        initializeParams.clientInfo = ClientInfo("Intellij Idea CangJie", "1.0.0")

        initializeParams.processId = ProcessHandle.current().pid().toInt()
        initializeParams.capabilities = getCapabilities()
        initializeParams.locale = "zh_cn"
        initializeParams.trace = "off"


        if (toolchain != null) {

            var projectName = project.name

            fun getMap(): Map<String, Any> {
                return mapOf(
                    "modulesHomeOption" to toolchain.location.systemIndependentPath.replacePathBySystem(),
//                    "extensionPath" to "C:\\Users\\27439\\.cangjie\\lsp"


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

    fun getCapabilities(): ClientCapabilities {
        val capabilities = ClientCapabilities()
        val workspace = WorkspaceClientCapabilities()
        workspace.applyEdit = true
//        val workspaceEdit = WorkspaceEdit()
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


}

fun Require.contentRoot(project: Project): VirtualFile? =
    if (path != null) {
        LocalFileSystem.getInstance().findFileByPath(path)
    } else {
        project.currentCjpmProject?.workspace?.packages?.find { `package` ->
            name == `package`.name
        }?.contentRoot
    }
