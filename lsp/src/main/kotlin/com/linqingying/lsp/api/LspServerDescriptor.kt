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

package com.linqingying.lsp.api

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.io.BaseDataReader
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.io.URLUtil
import com.linqingying.lsp.api.customization.*
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.jetbrains.annotations.ApiStatus
import java.net.URI
import java.net.URISyntaxException

/**
 * 定义如何启动 LSP 服务器进程（[startServerProcess]、[createCommandLine] 和 [lspCommunicationChannel]），
 * 以及如何与运行中的 LSP 服务器进行通信（所有其他函数和属性）。
 *
 * 请参阅 [LspServerSupportProvider] 文档，了解有关启动 LSP 服务器的详细信息。
 *
 * 希望为整个项目运行单个 LSP 服务器的插件，应该扩展 [ProjectWideLspServerDescriptor]。
 *
 * 通常，`LspServerDescriptor` 实现不会存储任何可修改的状态。
 *
 * 通常，插件不会保存对 `LspServerDescriptor` 实现的引用。要获取用于启动特定 LSP 服务器的 `LspServerDescriptor`，
 * 请使用 [LspServer.descriptor]，可以通过 [LspServerManager.getServersForProvider] 找到相应的 [LspServer]。
 *
 * 若要在 `Notifications` 工具窗口中查看所有来自服务器的 [window/logMessage](https://microsoft.github.io/language-server-protocol/specification/#window_logMessage)
 * 和 [$/logTrace](https://microsoft.github.io/language-server-protocol/specification/#traceValue) 通知，
 * 请在设置 -> 外观与行为 -> 通知中选择 `'LSP log: info, trace'` 类别的 `'在工具窗口中显示'` 复选框。
 *
 * @param presentableName 该字符串可能会出现在某些 UI 中，例如：
 * - `Language Services` 状态栏小部件项
 *   ([LspServerWidgetItem.getWidgetActionText][com.linqingying.lsp.api.lsWidget.LspServerWidgetItem.widgetActionText])
 * - `Show Usages` 弹出框标题
 * - `Notifications` 工具窗口中的 LSP 服务器相关信息
 */


abstract class LspServerDescriptor protected constructor(
    val project: Project,
    @NlsSafe val presentableName: String,
    vararg val roots: VirtualFile,
) {

    /**
     * 帮助读取日志。顺便说一句，实现类如果需要的话可以使用本类中定义的 [LOG] 属性。
     */
    override fun toString(): @NlsSafe String = javaClass.simpleName + "@" + project.name

    /**
     * 定义如何启动 LSP 服务器进程（[startServerProcess], [createCommandLine] 和 [lspCommunicationChannel]），
     * 以及如何与运行中的 LSP 服务器通信（所有其他函数和属性）。
     *
     * 有关启动 LSP 服务器的详细信息，请参阅 [LspServerSupportProvider] 文档。
     *
     * 希望在整个项目中运行单个 LSP 服务器的插件应扩展 [ProjectWideLspServerDescriptor]。
     *
     * 通常情况下，`LspServerDescriptor` 实现不存储任何可修改的状态。
     *
     * 作为规则，插件不会保留对 `LspServerDescriptor` 实现的引用。要获取用于启动特定 LSP 服务器的 `LspServerDescriptor`，
     * 可以使用 [LspServer.descriptor]，其中 [LspServer] 本身可以通过 [LspServerManager.getServersForProvider] 找到。
     *
     * 要在 `Notifications` 工具窗口中查看来自服务器的所有 [window/logMessage](https://microsoft.github.io/language-server-protocol/specification/#window_logMessage)
     * 和 [$/logTrace](https://microsoft.github.io/language-server-protocol/specification/#traceValue) 通知，请在设置 -> 外观与行为 -> 通知中
     * 选中 `'LSP log: info, trace'` 类别的 `'显示在工具窗口中'` 复选框。
     *
     * @param presentableName 这个字符串可能在某些情况下出现在 UI 中，例如：
     * - `Language Services` 状态栏小部件项
     * ([LspServerWidgetItem.getWidgetActionText][com.linqingying.lsp.api.lsWidget.LspServerWidgetItem.widgetActionText])
     * - `显示用法` 弹出窗口标题
     * - LSP 服务器相关的 `Notifications` 工具窗口中的信息
     */
    @RequiresReadLock
    abstract fun isSupportedFile(file: VirtualFile): Boolean

    /**
     * 启动 LSP 服务器进程。
     * 通常，插件无需重写此函数，只需实现 [createCommandLine] 函数。
     */
    @RequiresBackgroundThread
    @Throws(ExecutionException::class)
    open fun startServerProcess(): OSProcessHandler {
        // LSP 规范指出：“默认为 utf-8，这是目前唯一支持的编码”
        // 参见 https://microsoft.github.io/language-server-protocol/specification/#contentPart
        val startingCommandLine = createCommandLine().withCharset(Charsets.UTF_8)
        LOG.info("$this: 启动 LSP 服务器: $startingCommandLine")
        return object : OSProcessHandler(startingCommandLine) {
            override fun readerOptions(): BaseOutputReader.Options = object : BaseOutputReader.Options() {
                override fun policy(): BaseDataReader.SleepingPolicy = forMostlySilentProcess().policy()

                // 必须保留 '\r' 在 "\r\n" 换行符中。它们影响字符计数，必须与 `Content-Length` 匹配
                override fun splitToLines(): Boolean = false
            }
        }
    }

    /**
     * 启动服务器的命令行。
     * 每个插件必须实现此函数，除非：
     * - 插件实现了更通用的函数 [startServerProcess]
     * - 插件使用 [LspCommunicationChannel.Socket] 并将 `startProcess` 设置为 `false`
     */

    @RequiresBackgroundThread
    @Throws(ExecutionException::class)
    open fun createCommandLine(): GeneralCommandLine {
        // this text goes only to the IDE logs
        @Suppress("HardCodedStringLiteral", "DialogTitleCapitalization")
        throw ExecutionException("createCommandLine() function not implemented")
    }

    /**
     * IDE 和 LSP 服务器之间的通信通道：StdIO 或 Socket
     */

    open val lspCommunicationChannel: LspCommunicationChannel = LspCommunicationChannel.StdIO

    /**
     * 返回一个 [DocumentUri](https://microsoft.github.io/language-server-protocol/specification/#documentUri)，可以在各种
     * 发送到 LSP 服务器的请求中使用。
     * 默认实现简单地调用 [getFilePath] 并将其转换为 `file://...` URI。
     */

    open fun getFileUri(file: VirtualFile): String {
        val escapedPath = URLUtil.encodePath(getFilePath(file))
        val url = VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, escapedPath)
        val uri = VfsUtil.toUri(url)?.toString() ?: url
        return lowercaseWindowsDriveAndEscapeColon(uri)
    }

    /**
     * LSP 规范 [要求](https://microsoft.github.io/language-server-protocol/specification/#uri)
     * 所有服务器都能正确处理两种格式的 URI：`file:///C:/foo` 和 `file:///c%3A/foo`。
     *
     * VS Code 总是发送小写的 Windows 驱动器字母，并且总是转义冒号
     * （参见这个 [问题](https://github.com/microsoft/vscode-languageserver-node/issues/1280)
     * 和相关的 [拉取请求](https://github.com/microsoft/language-server-protocol/pull/1786)）。
     *
     * 一些 LSP 服务器仅支持 VS Code 友好的 URI 格式（`file:///c%3A/foo`），因此默认使用这种格式更安全。
     */
    private fun lowercaseWindowsDriveAndEscapeColon(uri: String): String {
        val prefix = "file:///"
        if (uri.startsWith(prefix) && OSAgnosticPathUtil.startsWithWindowsDrive(uri.substring(prefix.length))) {
            return prefix + uri[prefix.length].lowercase() + "%3A" + uri.substring(prefix.length + 2)
        }
        return uri
    }

    /**
     * @see getFileUri
     */
    protected open fun getFilePath(file: VirtualFile) = file.path

    /**
     * 从 [fileUri] 中提取文件路径并调用 [findLocalFileByPath]。仅支持 `file://...` URI。
     * @param fileUri 从 LSP 服务器在某些响应或通知中接收到的 [DocumentUri](https://microsoft.github.io/language-server-protocol/specification/#documentUri)
     */
    open fun findFileByUri(fileUri: String): VirtualFile? {
        val badWslUriStart = "file:///wsl\$/"
        val fixedFileUri = when {
            fileUri.startsWith(badWslUriStart) -> "file:////wsl\$/${fileUri.substring(badWslUriStart.length)}"
            else -> fileUri
        }

        return try {
            val uri = URI(fixedFileUri)
            if (URLUtil.FILE_PROTOCOL != uri.scheme) {
                LOG.warn("Unexpected URI scheme: $fileUri")
                return null
            }
            val path = uri.path
            if (path == null) {
                LOG.warn("Unexpected URI (no path): $fileUri")
                return null
            }
            findLocalFileByPath(path)
        } catch (e: URISyntaxException) {
            LOG.warn("Malformed URI: " + fileUri + "; " + e.message)
            null
        }
    }

    /**
     * @see findFileByUri
     */
    protected open fun findLocalFileByPath(path: String): VirtualFile? =
        LocalFileSystem.getInstance().findFileByPath(path)

    /**
     * 返回 [TextDocumentItem](https://microsoft.github.io/language-server-protocol/specification#textDocumentItem) 类的 `languageId` 字段，
     * 该字段在 [textDocument/didOpen](https://microsoft.github.io/language-server-protocol/specification#textDocumentItem)
     * 通知中是必需的。`languageId` 通常等于小写的文件扩展名。标准实现还处理了一些已知的
     * 例外情况。
     */

    open fun getLanguageId(file: VirtualFile): String = Companion.getLanguageId(file)

    /**
     * [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams) 对象作为 [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) 请求
     * 发送到 LSP 服务器。
     */
    open fun createInitializeParams(): InitializeParams = InitializeParams().apply {
        clientInfo = ClientInfo(
            ApplicationNamesInfo.getInstance().fullProductNameWithEdition,
            ApplicationInfo.getInstance().build.asStringWithoutProductCode()
        )

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

    /**
     * [ClientCapabilities](https://microsoft.github.io/language-server-protocol/specification/#clientCapabilities) 是 [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams) 类中的一个必填字段，
     * 它作为 [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) 请求发送到 LSP 服务器。
     *
     * 插件可以重写此属性，并根据需要调整功能。
     * 例如，插件可能需要将 [workspace.configuration][org.eclipse.lsp4j.WorkspaceClientCapabilities.setConfiguration] 能力设置为 `true`，
     * 并在重写的 [getWorkspaceConfiguration] 函数中提供所需的实现。
     */
    open val clientCapabilities: ClientCapabilities
        get() = ClientCapabilities().apply {
            workspace = WorkspaceClientCapabilities().apply {
                applyEdit = true
                workspaceFolders = true
                //configuration = true // 默认为 false，因为 [getWorkspaceConfiguration] 默认返回 null
                workspaceEdit = WorkspaceEditCapabilities().apply {
                    documentChanges = true
                    resourceOperations = listOf(ResourceOperationKind.Create)
                    failureHandling = FailureHandlingKind.Abort
                    normalizesLineEndings = true
                }
                didChangeWatchedFiles = DidChangeWatchedFilesCapabilities(true).apply {
                    relativePatternSupport = true
                }
                executeCommand = ExecuteCommandCapabilities(false)
                semanticTokens = SemanticTokensWorkspaceCapabilities().apply {
                    refreshSupport = true
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
                        snippetSupport = true
                        resolveSupport = CompletionItemResolveSupportCapabilities().apply {
                            properties = listOf("documentation")
                        }
                    }
                    completionItemKind = CompletionItemKindCapabilities().apply {
                        valueSet = CompletionItemKind.entries
                    }
                    completionList = CompletionListCapabilities().apply {
                        itemDefaults =
                            listOf("commitCharacters", "editRange", "insertTextFormat", "insertTextMode", "data")
                    }
                }
                hover = HoverCapabilities().apply {
                    contentFormat = listOf(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT)
                }

                val semanticTokensSupport = lspSemanticTokensSupport
                if (semanticTokensSupport != null) {
                    semanticTokens = SemanticTokensCapabilities().apply {
                        requests = SemanticTokensClientCapabilitiesRequests().apply {
                            range = Either.forLeft(false)
                            full = Either.forRight(SemanticTokensClientCapabilitiesRequestsFull().apply {
                                delta = false
                            })
                            tokenTypes = semanticTokensSupport.tokenTypes
                            tokenModifiers = semanticTokensSupport.tokenModifiers
                            formats = listOf(TokenFormat.Relative)
                            overlappingTokenSupport = true
                            multilineTokenSupport = true
                            serverCancelSupport = false
                        }
                    }
                }

                publishDiagnostics = PublishDiagnosticsCapabilities().apply {
                    versionSupport = true
                    tagSupport = Either.forRight(
                        DiagnosticsTagSupport(
                            listOf(
                                DiagnosticTag.Unnecessary,
                                DiagnosticTag.Deprecated
                            )
                        )
                    )
                }
                codeAction = CodeActionCapabilities().apply {
                    codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
                        codeActionKind = CodeActionKindCapabilities(
                            listOf(
                                CodeActionKind.QuickFix,
                                CodeActionKind.Empty,
                                CodeActionKind.Source,
                                CodeActionKind.Refactor,
                            )
                        )
                    }
                    disabledSupport = true
                    dataSupport = true
                    resolveSupport = CodeActionResolveSupportCapabilities().apply {
                        properties = listOf("edit")
                    }
                }
                formatting = FormattingCapabilities(true)
                references = ReferencesCapabilities(true)
                colorProvider = ColorProviderCapabilities(true)
            }
            notebookDocument = null
            window = WindowClientCapabilities().apply {
                showMessage = WindowShowMessageRequestCapabilities()
                showDocument = ShowDocumentCapabilities(true)
            }
            general = GeneralClientCapabilities().apply {
                staleRequestSupport = StaleRequestCapabilities().apply {
                    isCancel = true
                }
            }
            experimental = null
        }

    /**
     * `initializationOptions` 是 [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams) 类中的一个可选字段，
     * 它作为 [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) 请求发送到 LSP 服务器。
     */
    open fun createInitializationOptions(): Any? = null

    /**
     * 创建 [org.eclipse.lsp4j.services.LanguageClient] 接口的实现。
     * 它处理 LSP 服务器发送到 IDE 的所有标准请求和通知。
     * '标准' 请求和通知是指在官方 [LSP 规范](https://microsoft.github.io/language-server-protocol/specification) 中记录的内容。
     *
     * 要处理来自服务器的自定义未记录请求/通知，插件可以重写此函数
     * 并返回其子类的 [Lsp4jClient] 类。
     * 有关更多信息，请参阅 [Lsp4jClient] 类文档。
     */
    open fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient = Lsp4jClient(handler)

    /**
     * 返回应作为此 [LspServer] 的 [Lsp4jServer] 使用的类。
     *
     * 如果插件需要向 LSP 服务器发送一些自定义的未记录通知或请求，插件需要定义一个 [Lsp4jServer] 接口的自定义子接口。
     *
     * 请注意，不需要实现这样的自定义子接口。
     * `lsp4j` 库将使用 Java 反射（[Proxy][java.lang.reflect.Proxy]）根据 `lsp4j` 特定的注解生成接口实现。
     *
     * 示例：
     *
     *    interface FooLsp4jServer : Lsp4jServer {
     *      @JsonRequest("foo/customRequest")
     *      fun customRequest(params: CustomParams): CompletableFuture<CustomResult>
     *
     *      @JsonNotification("foo/customNotification")
     *      fun customNotification(params: CustomNotificationParams)
     *    }
     *
     * @see [LspServer.sendNotification]
     * @see [LspServer.sendRequest]
     * @see [LspServer.sendRequestSync]
     */
    open val lsp4jServerClass: Class<out Lsp4jServer> = Lsp4jServer::class.java

    /**
     * 插件可以提供监听器，以便在发生 [LspServer] 事件时收到通知。
     */
    open val lspServerListener: LspServerListener? = null

    /**
     * 指示 IntelliJ 的 `Navigate -> Declaration` 操作是否应通过发送 [textDocument/definition](https://microsoft.github.io/language-server-protocol/specification/#textDocument_definition) 请求
     * 来使用来自 LSP 服务器的数据。
     */
    open val lspGoToDefinitionSupport = true

    /**
     * 是否应该通过发送 [textDocument/hover](https://microsoft.github.io/language-server-protocol/specification/#textDocument_hover) 请求
     * 向 LSP 服务器请求悬停信息。
     */
    open val lspHoverSupport = true

    /**
     * 默认情况下，使用标准的 [LspCompletionSupport] 实现。
     * 实现可以重写此属性并返回其特定子类的 [LspCompletionSupport]，以微调代码补全行为（例如，为每个补全项提供特殊图标）。
     * 此外，如果该 LSP 服务器不需要使用内置的代码补全支持，实现可以返回 `null`。
     */
    open val lspCompletionSupport: LspCompletionSupport? = LspCompletionSupport()

    /**
     * 请参阅 LSP 规范：
     * [语义标记](https://microsoft.github.io/language-server-protocol/specification/#textDocument_semanticTokens)
     *
     * 插件可以使用来自 LSP 服务器的信息以及其他方法（例如 TextMate Bundles）提供基本的语法高亮。
     *
     * - 实现可以返回 `null` 来禁用基于服务器的语义高亮
     * - 实现可以重写 `LspSemanticTokensSupport` 类来处理特定于服务器的语义标记
     * - 可以按文件控制此功能，详见 [LspSemanticTokensSupport.shouldAskServerForSemanticTokens]
     */
    open val lspSemanticTokensSupport: LspSemanticTokensSupport? = LspSemanticTokensSupport()
    /**
     * 默认情况下，使用标准的 [LspDiagnosticsSupport] 实现来处理来自 LSP 服务器的
     * [textDocument/publishDiagnostics](https://microsoft.github.io/language-server-protocol/specification/#textDocument_publishDiagnostics)
     * 通知。实现可以重写此属性并返回其特定子类的 [LspDiagnosticsSupport]，
     * 以细化错误和警告以及相应的快速修复。同时，若不需要使用内置的诊断支持，
     * 实现可以返回 `null`。
     */
    open val lspDiagnosticsSupport: LspDiagnosticsSupport? = LspDiagnosticsSupport()

    open val lspCodeActionsSupport: LspCodeActionsSupport? = LspCodeActionsSupport()

    /**
     * 处理从 LSP 服务器接收到的 [Command](https://microsoft.github.io/language-server-protocol/specification#command) 对象。
     * @see LspCommandsSupport.executeCommand
     */
    open val lspCommandsSupport: LspCommandsSupport? = LspCommandsSupport()

    /**
     * 帮助决定是否应该为每个特定文件使用 LSP 服务器进行代码格式化。
     * 实现可以返回 `null` 来完全禁用基于服务器的代码格式化。
     */
    open val lspFormattingSupport: LspFormattingSupport? = LspFormattingSupport()

    /**
     * 如果不为 `null`，则 `查找用法` 和 `显示用法` 功能将使用
     * [textDocument/references](https://microsoft.github.io/language-server-protocol/specification/#textDocument_references)
     * 请求来获取结果。
     */
    open val lspFindReferencesSupport: LspFindReferencesSupport? = LspFindReferencesSupport()

    /**
     * 如果不为 `null`，则 IDE 将使用
     * [textDocument/documentColor](https://microsoft.github.io/language-server-protocol/specification/#textDocument_documentColor)
     * 请求来装饰编辑器中的颜色引用。
     */
    open val lspDocumentColorSupport: LspDocumentColorSupport? = LspDocumentColorSupport()

    /**
     * 处理来自服务器的
     * [workspace/configuration](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_configuration)
     * 请求。
     *
     * 重写此函数的插件可能还应该重写 [clientCapabilities] 属性，
     * 并将 [workspace.configuration][org.eclipse.lsp4j.WorkspaceClientCapabilities.setConfiguration] 能力设置为 `true`。
     */
    open fun getWorkspaceConfiguration(item: ConfigurationItem): Any? = null

    companion object {
        @JvmField
        val LOG: Logger = Logger.getInstance(LspServerDescriptor::class.java)

        /**
         * 获取文件的 `languageId`，该 ID 通常对应文件扩展名的小写形式。
         * @param file 要获取语言 ID 的文件
         * @return 对应的语言 ID
         */
        fun getLanguageId(file: VirtualFile): String {
            val nameLowercased = StringUtil.toLowerCase(file.name)
            // 检查文件名后缀是否有对应的语言 ID
            FILE_NAME_ENDING_TO_LANGUAGE_ID.find { nameLowercased.endsWith(it.first) }?.let { return it.second }
            // 根据扩展名查找语言 ID
            return StringUtil.toLowerCase(file.extension)?.let { FILE_EXTENSION_TO_LANGUAGE_ID[it] ?: it } ?: ""
        }

        // 文件名后缀到语言 ID 的映射
        private val FILE_NAME_ENDING_TO_LANGUAGE_ID: List<Pair<String, String>> = listOf(
            ".blade.php" to "blade"
        )

        // 根据 LSP 规范，语言 ID 通常等于小写的文件扩展名。
        // 该映射只包含非标准情况。
        private val FILE_EXTENSION_TO_LANGUAGE_ID: Map<String, String> = mapOf(
            "js" to "javascript",
            "cjs" to "javascript",
            "mjs" to "javascript",
            "jsx" to "javascriptreact",
            "ts" to "typescript",
            "tsx" to "typescriptreact",

            "fs" to "fsharp",
            "fsx" to "fsharp",
            "handlebars" to "handlebars",
            "pcss" to "postcss",
            "phtml" to "php",
            "pug" to "jade",
            "py" to "python",
            "rb" to "ruby",
            "tpl" to "smarty",
        )
    }

}
