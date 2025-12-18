/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.lsp4ij

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.io.systemIndependentPath
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.lsp.replacePathBySystem
import org.cangnova.cangjie.project.model.*
import org.cangnova.cangjie.project.service.CjDependencyService
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.eclipse.lsp4j.*
import java.net.URI

internal class CangJieLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return CangJieOSProcessStreamConnectionProvider(project)
    }

    override fun createClientFeatures(): LSPClientFeatures {
        return CangJieLSPClientFeatures()
    }
}


/**
 * 将VirtualFile转换为URI字符串
 */
fun toString(virtualFile: VirtualFile): String {
    val path = virtualFile.toNioPathOrNull()
    return if (path != null) {
        path.toUri().toString()
    } else {
        // Fallback for when toNioPathOrNull returns null
        val uri = URI.create("file://" + virtualFile.path.replace("\\", "/"))
        uri.toString()
    }
}

class CangJieLSPClientFeatures : LSPClientFeatures() {

    init {
        setFileUriSupport(ENCODED)
        diagnosticFeature = CangJieLSPDiagnosticFeature()
        hoverFeature = CangJieLSPHoverFeature()
        codeLensFeature = CangJieLSPCodeLensFeature
    }


    private fun getCapabilities(): ClientCapabilities {
        val capabilities = ClientCapabilities()
        val workspace = WorkspaceClientCapabilities()
        workspace.applyEdit = true

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
        val requests = SemanticTokensClientCapabilitiesRequests()
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
        capabilities.general = general
        val notebookDocument = NotebookDocumentClientCapabilities()
        val synchronizationCapabilities1 = NotebookDocumentSyncClientCapabilities()
        synchronizationCapabilities1.dynamicRegistration = true

        notebookDocument.synchronization = synchronizationCapabilities1

        capabilities.notebookDocument = notebookDocument

        return capabilities

    }

    /**
     * 创建 WorkspaceFolders 信息
     * 根据项目类型（单模块/工作空间）返回适当的工作空间文件夹列表
     */
    private fun createWorkspaceFolders(): List<WorkspaceFolder> {
        val cjProject = project.cjProject
        val folders = mutableListOf<WorkspaceFolder>()

        if (cjProject.isWorkspace) {
            // 工作空间项目：为每个模块创建一个 WorkspaceFolder
            cjProject.workspace?.modules?.forEach { module ->
                folders.add(WorkspaceFolder().apply {
                    uri = toString(module.rootDir)
                    name = module.name
                })
            }
        } else {
            // 单模块项目：为项目根目录创建一个 WorkspaceFolder
            cjProject.module?.let { module ->
                folders.add(WorkspaceFolder().apply {
                    uri = toString(module.rootDir)
                    name = module.name
                })
            }
        }

        // 如果没有找到任何模块，至少添加项目根目录
        if (folders.isEmpty()) {
            folders.add(WorkspaceFolder().apply {
                uri = toString(project.baseDir)
                name = project.name
            })
        }

        return folders
    }


    override fun initializeParams(initializeParams: InitializeParams) {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        initializeParams.clientInfo = ClientInfo("Intellij CangJie", "1.0.0")
        initializeParams.processId = ProcessHandle.current().pid().toInt()
        initializeParams.capabilities = getCapabilities()
        initializeParams.locale = "zh_cn"
        initializeParams.trace = "off"

        if (initializeParams.rootUri == null) {
            toString(project.baseDir)?.let {
                initializeParams.rootUri = it
            }

        }
        if (initializeParams.rootPath == null) {
            initializeParams.rootPath = project.basePath


        }

        // 添加 WorkspaceFolders 信息
        initializeParams.workspaceFolders = createWorkspaceFolders()

        if (sdk != null) {
            // 使用cangjie-project模型创建initializationOptions
            initializeParams.initializationOptions = createInitializationOptions(sdk)
        }
    }

    /**
     * 使用cangjie-project模型创建initializationOptions
     * 根据LSP服务器的要求格式化配置选项
     *
     * 注意：LSP服务器是单项目单实例模式，一个CjProject对应一个LSP服务器
     */
    private fun createInitializationOptions(sdk: CjSdk): Map<String, Any> {

//仓颉lsp不支持同时处理多个项目
        // 获取当前项目（每个LSP服务器实例应该只对应一个CjProject）
        val currentProject = project.cjProject

        return mutableMapOf<String, Any>().apply {
            // 核心配置选项
            put("modulesHomeOption", sdk.homePath.systemIndependentPath.replacePathBySystem())

//            // 标准库路径（如果SDK有提供）
            put("stdLibPathOption", getStdLibPath(sdk))

            // 目标库路径（用于宏库路径）
            put("targetLib", getTargetLibPath(currentProject))

            // 多模块配置 - 基于当前项目的模块
            put(
                "multiModuleOption",
                createMultiModuleOption(currentProject)
            )
//
            // 条件编译配置
            put("conditionCompileOption", mutableMapOf<String, String>())
            put("singleConditionCompileOption", mutableMapOf<String, Map<String, String>>())
            put("conditionCompilePaths", getConditionCompilePaths(currentProject))

            // DevEco IDE专用配置（如果需要）
            put("cjdCachePathOption", getCjdCachePath(currentProject))
        }
    }

    /**
     * 获取标准库路径
     */
    private fun getStdLibPath(sdk: CjSdk): String {
        return sdk.stdlibPath.systemIndependentPath
    }

    /**
     * 获取目标库路径
     */
    private fun getTargetLibPath(project: CjProject?): String {
        return project?.rootDir?.path?.let {
            "$it/.cache/lsp"
        } ?: ""
    }


    /**
     * 为模块列表创建配置选项
     */
    private fun createMultiModuleOption(cjProject: CjProject): Map<String, Any> {
        val modules = if (cjProject.isWorkspace) {
            cjProject.workspace?.modules ?: emptyList()
        } else {
            cjProject.module?.let { listOf(it) } ?: emptyList()
        }

        if (modules.isEmpty()) return mutableMapOf()

        // 收集所有需要配置的模块信息（主模块 + 依赖模块）
        val allModuleInfos = mutableMapOf<String, ModuleConfigInfo>()

        // 首先收集主模块（优先级最高）
        for (module in modules) {
            val moduleUri = toString(module.rootDir)
            allModuleInfos[moduleUri] = ModuleConfigInfo.Main(module)
        }

        // 然后收集依赖模块（仅源码依赖：Git、Path）
        val externalDependencies = mutableMapOf<String, DependencyModuleInfo>()
        for (module in modules) {
            collectSourceDependencies(module, externalDependencies)
        }

        // 将依赖模块添加到 allModuleInfos（跳过已存在的主模块）
        for ((depUri, depInfo) in externalDependencies) {
            if (!allModuleInfos.containsKey(depUri)) {
                allModuleInfos[depUri] = ModuleConfigInfo.Dependency(depInfo)
            }
        }

        // 统一生成配置
        return allModuleInfos.mapValues { (_, moduleInfo) ->
            when (moduleInfo) {
                is ModuleConfigInfo.Main -> createModuleConfig(moduleInfo.module, externalDependencies)
                is ModuleConfigInfo.Dependency -> createDependencyModuleConfig(moduleInfo.info)
            }
        }
    }

    /**
     * 模块配置信息的密封类
     *
     * 用于统一表示主模块和依赖模块，便于统一处理
     */
    private sealed class ModuleConfigInfo {
        /**
         * 主模块（工作空间中的模块或单模块项目）
         */
        data class Main(val module: CjModule) : ModuleConfigInfo()

        /**
         * 依赖模块（Git/Path 源码依赖）
         */
        data class Dependency(val info: DependencyModuleInfo) : ModuleConfigInfo()
    }

    /**
     * 依赖模块信息
     */
    private data class DependencyModuleInfo(
        val name: String,
        val rootPath: VirtualFile,
        val dependency: CjDependency,
        val resolvedPackage: CjPackage? = null  // 已解析的包信息（用于获取传递依赖）
    )

    /**
     * 收集模块的所有源码依赖（Git、Path 依赖），包括传递依赖
     */
    private fun collectSourceDependencies(
        module: CjModule,
        dependencyModules: MutableMap<String, DependencyModuleInfo>
    ) {
        val allDeps = module.dependencies + module.buildDependencies + module.testDependencies
        collectSourceDependenciesRecursive(allDeps, dependencyModules, mutableSetOf())
    }

    /**
     * 递归收集源码依赖，包括传递依赖
     *
     * @param dependencies 当前层级的依赖列表
     * @param dependencyModules 已收集的依赖模块映射
     * @param visited 已访问的依赖路径集合（用于防止循环依赖）
     */
    private fun collectSourceDependenciesRecursive(
        dependencies: List<CjDependency>,
        dependencyModules: MutableMap<String, DependencyModuleInfo>,
        visited: MutableSet<String>
    ) {
        for (dep in dependencies) {
            when (dep) {
                is CjDependency.Git -> {
                    // Git 依赖：解析并添加到依赖模块列表
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Git) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.checkoutPath)
                        virtualFile?.let {
                            val uri = toString(it)

                            // 防止重复处理和循环依赖
                            if (visited.contains(uri)) return@let
                            visited.add(uri)

                            if (!dependencyModules.containsKey(uri)) {
                                val depInfo = DependencyModuleInfo(
                                    name = dep.name,
                                    rootPath = it,
                                    dependency = dep,
                                    resolvedPackage = resolvedPackage
                                )
                                dependencyModules[uri] = depInfo

                                // 递归处理传递依赖
                                val transitiveDeps = getTransitiveDependencies(resolvedPackage)
                                if (transitiveDeps.isNotEmpty()) {
                                    collectSourceDependenciesRecursive(transitiveDeps, dependencyModules, visited)
                                }
                            }
                        }
                    }
                }
                is CjDependency.Path -> {
                    // Path 依赖：解析并添加到依赖模块列表
                    // 注意：dep.path 可能是相对路径（如 ../abc），需要先解析为绝对路径
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Path) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.path)
                        virtualFile?.let {
                            val uri = toString(it)

                            // 防止重复处理和循环依赖
                            if (visited.contains(uri)) return@let
                            visited.add(uri)

                            if (!dependencyModules.containsKey(uri)) {
                                val depInfo = DependencyModuleInfo(
                                    name = dep.name,
                                    rootPath = it,
                                    dependency = dep,
                                    resolvedPackage = resolvedPackage
                                )
                                dependencyModules[uri] = depInfo

                                // 递归处理传递依赖
                                val transitiveDeps = getTransitiveDependencies(resolvedPackage)
                                if (transitiveDeps.isNotEmpty()) {
                                    collectSourceDependenciesRecursive(transitiveDeps, dependencyModules, visited)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Library 和 Binary 依赖不需要在这里处理
                }
            }
        }
    }

    /**
     * 获取已解析包的传递依赖
     */
    private fun getTransitiveDependencies(pkg: CjPackage): List<CjDependency> {
        return when (pkg) {
            is CjPackage.Git -> {
                // 从 Git 包中获取依赖信息
                // Git 包解析后应该包含依赖信息
                pkg.dependencies
            }
            is CjPackage.Path -> {
                // Path 包的传递依赖
                // 依赖解析器已经从 cjpm.toml 中读取并解析了传递依赖
                pkg.dependencies
            }
            is CjPackage.Library -> {
                // Library 包的传递依赖
                pkg.dependencies
            }
            else -> emptyList()
        }
    }

    /**
     * 创建主模块配置
     */
    private fun createModuleConfig(
        module: CjModule,
        dependencyModules: Map<String, DependencyModuleInfo>
    ): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("name", module.name)
            put("package_requires", createPackageRequires(module))
            put("source_sets", createSourceSetsInfo(module))

            // 添加 requires 字段，声明源码依赖关系
            val requires = createRequires(module, dependencyModules)
            if (requires.isNotEmpty()) {
                put("requires", requires)
            }
        }
    }

    /**
     * 创建依赖模块配置
     */
    private fun createDependencyModuleConfig(depInfo: DependencyModuleInfo): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("name", depInfo.name)

            // 源码集配置：推断标准目录结构
            put("source_sets", mutableMapOf<String, Any>().apply {
                put("main", mutableMapOf<String, Any>().apply {
                    val srcDir = depInfo.rootPath.findChild("src")
                    put("source_roots", if (srcDir != null) listOf(toString(srcDir)) else emptyList())
                    put("resource_roots", emptyList<String>())
                    put("output_directory", emptyList<String>())
                    put("is_test", false)
                })
            })

            // package_requires：处理依赖模块的 Library 和 Binary 依赖
            val transitiveDeps = depInfo.resolvedPackage?.let { getTransitiveDependencies(it) } ?: emptyList()
            put("package_requires", createPackageRequiresForDependency(transitiveDeps))

            // requires：处理依赖模块的 Git/Path 源码依赖
            val requires = createRequiresForDependency(transitiveDeps)
            if (requires.isNotEmpty()) {
                put("requires", requires)
            }
        }
    }

    /**
     * 为依赖模块创建 package_requires 配置
     */
    private fun createPackageRequiresForDependency(dependencies: List<CjDependency>): Map<String, Any> {
        val pathOptions = mutableSetOf<String>()
        val packageOptions = mutableMapOf<String, String>()

        processDependencies(dependencies, pathOptions, packageOptions)

        return mutableMapOf<String, Any>().apply {
            put("path_option", pathOptions.toList())
            put("package_option", packageOptions)
        }
    }

    /**
     * 为依赖模块创建 requires 字段
     */
    private fun createRequiresForDependency(dependencies: List<CjDependency>): Map<String, Map<String, String>> {
        val requires = mutableMapOf<String, Map<String, String>>()

        for (dep in dependencies) {
            when (dep) {
                is CjDependency.Git -> {
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Git) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.checkoutPath)
                        virtualFile?.let {
                            val uri = toString(it)
                            requires[dep.name] = mapOf("path" to uri)
                        }
                    }
                }
                is CjDependency.Path -> {
                    // 解析 Path 依赖以处理相对路径
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Path) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.path)
                        virtualFile?.let {
                            val uri = toString(it)
                            requires[dep.name] = mapOf("path" to uri)
                        }
                    }
                }
                else -> {
                    // Library 和 Binary 依赖不在 requires 中
                }
            }
        }

        return requires
    }

    /**
     * 创建 requires 字段，声明源码依赖关系
     */
    private fun createRequires(
        module: CjModule,
        dependencyModules: Map<String, DependencyModuleInfo>
    ): Map<String, Map<String, String>> {
        val requires = mutableMapOf<String, Map<String, String>>()
        val allDeps = module.dependencies + module.buildDependencies + module.testDependencies

        for (dep in allDeps) {
            when (dep) {
                is CjDependency.Git -> {
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Git) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.checkoutPath)
                        virtualFile?.let {
                            val uri = toString(it)
                            requires[dep.name] = mapOf("path" to uri)
                        }
                    }
                }
                is CjDependency.Path -> {
                    // 解析 Path 依赖以处理相对路径
                    val resolvedPackage = resolveDependency(project, dep)
                    if (resolvedPackage is CjPackage.Path) {
                        val virtualFile = VirtualFileManager.getInstance()
                            .findFileByNioPath(resolvedPackage.path)
                        virtualFile?.let {
                            val uri = toString(it)
                            requires[dep.name] = mapOf("path" to uri)
                        }
                    }
                }
                else -> {
                    // Library 和 Binary 依赖不在 requires 中
                }
            }
        }

        return requires
    }

    /**
     * 创建模块的依赖配置
     * 将所有类型的依赖转换为 LSP 服务器期望的格式
     */
    private fun createPackageRequires(module: CjModule): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            // 收集所有路径依赖（使用 Set 自动去重）
            val pathOptions = mutableSetOf<String>()
            val packageOptions = mutableMapOf<String, String>()

            // 处理编译依赖
            processDependencies(module.dependencies, pathOptions, packageOptions)

            // 处理构建依赖
            processDependencies(module.buildDependencies, pathOptions, packageOptions)

            // 处理测试依赖
            processDependencies(module.testDependencies, pathOptions, packageOptions)



            put("path_option", pathOptions.toList())
            put("package_option", packageOptions)
        }
    }

    /**
     * 处理依赖列表，将其转换为 LSP 格式
     *
     * 注意：
     * - Git/Path 依赖（源码依赖）会在 multiModuleOption 顶层声明，不在 path_option 中
     * - Binary 依赖放在 path_option 中（LSP 会扫描 .cjo 文件）
     * - Library 依赖放在 package_option 中
     */
    private fun processDependencies(
        dependencies: List<CjDependency>,
        pathOptions: MutableSet<String>,
        packageOptions: MutableMap<String, String>
    ) {
        dependencies.forEach { dependency ->
            when (dependency) {
                is CjDependency.Library -> {
                    // 库依赖：添加到 package_option
                    val depId = if (dependency.group != null) {
                        "${dependency.group}:${dependency.name}"
                    } else {
                        dependency.name
                    }
                    packageOptions[depId] = dependency.versionReq.toString()
                }

                is CjDependency.Path -> {
                    // 路径依赖（源码）：已在 multiModuleOption 中声明，这里不处理
                    // 源码依赖会通过 collectSourceDependencies 和 createRequires 处理
                }

                is CjDependency.Git -> {
                    // Git 依赖（源码）：已在 multiModuleOption 中声明，这里不处理
                    // 源码依赖会通过 collectSourceDependencies 和 createRequires 处理
                }

                is CjDependency.Binary -> {
                    // 二进制依赖：添加包含 .cjo 文件的目录到 path_option
                    // LSP 会扫描该目录下的所有 .cjo 文件
                    val virtualFile = VirtualFileManager.getInstance()
                        .findFileByNioPath(dependency.cjoPath)
                    virtualFile?.let {
                        val parent = it.parent
                        if (parent != null) {
                            pathOptions.add(toString(parent))
                        }
                    }
                }

                is  CjDependency.Stdlib -> {
                    // 标准库依赖：由 SDK 的 stdLibPathOption 处理，这里不需要额外配置
                }
            }
        }
    }

    /**
     * 解析依赖，获取依赖的详细信息
     *
     * 使用 CjDependencyService 来解析依赖
     */
    private fun resolveDependency(project: Project, dependency: CjDependency): CjPackage? {
        val dependencyService = CjDependencyService.getInstance(project)
        return try {
            // 使用 runBlocking 在同步上下文中调用挂起函数
            runBlocking {
                dependencyService.resolveSingle(dependency).getOrNull()
            }
        } catch (e: Exception) {
            // 解析失败时返回 null，这样 LSP 会跳过这个 Git 依赖
            null
        }
    }

    /**
     * 创建源码集信息
     */
    private fun createSourceSetsInfo(module: CjModule): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            module.sourceSets.forEach { sourceSet ->
                put(sourceSet.name, mutableMapOf<String, Any>().apply {
                    put("source_roots", sourceSet.sourceRoots.map { toString(it) })
                    put("resource_roots", sourceSet.resourceRoots.map { toString(it) })
                    put("output_directory", sourceSet.outputDirectory.map { toString(it) })
                    put("is_test", sourceSet.isTest)
                })
            }
        }
    }

    /**
     * 获取条件编译路径
     */
    private fun getConditionCompilePaths(project: CjProject?): List<String> {
        return project?.let { listOf(it.rootDir.path) } ?: emptyList()
    }

    /**
     * 获取CJD缓存路径
     */
    private fun getCjdCachePath(project: CjProject?): String {
        return project?.rootDir?.path?.let {
            "$it/.idea/cjdIdx"
        } ?: ""
    }
}


