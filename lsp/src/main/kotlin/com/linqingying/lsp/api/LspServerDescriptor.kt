
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
import com.linqingying.lsp.api.customization.*
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.io.URLUtil
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.jetbrains.annotations.ApiStatus
import java.net.URI
import java.net.URISyntaxException

/**
 * Defines how to start the LSP server process ([startServerProcess], [createCommandLine], and [lspCommunicationChannel]),
 * and how to communicate with the running LSP server (all other functions and properties).
 *
 * See [LspServerSupportProvider] documentation for details about starting an LSP server.
 *
 * Plugins that want to run a single LSP server for the whole project, regardless of the project structure, should extend
 * [ProjectWideLspServerDescriptor].
 *
 * Normally, `LspServerDescriptor` implementations don't store any modifiable state.
 *
 * As a rule, plugins don't keep references to the `LspServerDescriptor` implementations. To get an `LspServerDescriptor` that is used to
 * start a specific LSP server, use [LspServer.descriptor], where the [LspServer] itself could be found using
 * [LspServerManager.getServersForProvider].
 *
 * To see all [window/logMessage](https://microsoft.github.io/language-server-protocol/specification/#window_logMessage)
 * and [$/logTrace](https://microsoft.github.io/language-server-protocol/specification/#traceValue) notifications from the server in the
 * `Notifications` tool window, select the `'Show in tool window'` check box for the `'LSP log: info, trace'` category
 * in Settings -> Appearance & Behavior -> Notifications.
 *
 * @param presentableName this string may appear in UI in some cases, for example:
 * - `Language Services` status bar widget item
 * ([LspServerWidgetItem.getWidgetActionText][com.linqingying.lsp.api.lsWidget.LspServerWidgetItem.widgetActionText])
 * - `Show Usages` popup header
 * - LSP server-related information in the `Notifications` tool window
 */

abstract class LspServerDescriptor protected constructor(
  val project: Project,
  @NlsSafe val presentableName: String,
  vararg val roots: VirtualFile,
) {

  /**
   * Helps to read logs. By the way, implementations can use [LOG] property defined in this class if they want.
   */
  override fun toString(): @NlsSafe String = javaClass.simpleName + "@" + project.name

  /**
   * Implementations should return `true` if the LSP server needs to track the file contents while the file is being edited.
   * In other words, `true` means that the server needs to know the maybe-not-yet-saved state of the file at any moment of time.
   * In this case the IDE will take care of sending the `didOpen`, `didChange`, and `didClose` notifications to the server
   * according to the
   * [specification](https://microsoft.github.io/language-server-protocol/specification/#textDocument_synchronization).
   *
   * If implementation returns `false` then the server will only know the contents of the file as it is stored on the disk.
   *
   * The implementation must be idempotent, have no side effects,
   * and the result must depend only on the given file but not on the project settings or whatever else.
   * The call site has the right to cache the returned result.
   *
   * Typical implementation:
   *
   *    override fun isSupportedFile(file: VirtualFile) = file.extension == "foo"
   * or
   *
   *    override fun isSupportedFile(file: VirtualFile) = file.fileType == FooFileType.INSTANCE
   *
   * @param file the file is guaranteed to be valid, in a local file system, and within project content roots. However, it might be not
   *             within the roots configured for this LSP server, which is usually fine.
   */
  @RequiresReadLock
  abstract fun isSupportedFile(file: VirtualFile): Boolean

  /**
   * Starts the LSP server process.
   * Usually, plugins don't need to override this function, but only implement the [createCommandLine] function.
   */
  @RequiresBackgroundThread
  @Throws(ExecutionException::class)
  open fun startServerProcess(): OSProcessHandler {
    // LSP spec says: "It defaults to utf-8, which is the only encoding supported right now"
    // see https://microsoft.github.io/language-server-protocol/specification/#contentPart
    val startingCommandLine = createCommandLine().withCharset(Charsets.UTF_8)
    LOG.info("$this: starting LSP server: $startingCommandLine")
    return object : OSProcessHandler(startingCommandLine) {
      override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.forMostlySilentProcess()
    }
  }

  /**
   * The command line to start the server.
   * Each plugin must implement this function, unless:
   * - the plugin implements the more generic function [startServerProcess]
   * - the plugin uses [LspCommunicationChannel.Socket] with `startProcess` set to `false`
   */
  @RequiresBackgroundThread
  @Throws(ExecutionException::class)
  open fun createCommandLine(): GeneralCommandLine {
    // this text goes only to the IDE logs
    @Suppress("HardCodedStringLiteral", "DialogTitleCapitalization")
    throw ExecutionException("createCommandLine() function not implemented")
  }

  /**
   * The channel for the communication between the IDE and the LSP server: StdIO or Socket
   */
  open val lspCommunicationChannel: LspCommunicationChannel = LspCommunicationChannel.StdIO

  /**
   * Returns a [DocumentUri](https://microsoft.github.io/language-server-protocol/specification/#documentUri), which can be used in various
   * requests to the LSP server.
   * The default implementation simply calls [getFilePath] and converts it to `file://...` URI.
   */
  open fun getFileUri(file: VirtualFile): String {
    val escapedPath = URLUtil.encodePath(getFilePath(file))
    val url = VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, escapedPath)
    val uri = VfsUtil.toUri(url)?.toString() ?: url
    return lowercaseWindowsDriveAndEscapeColon(uri)
  }

  /**
   * The LSP spec [requires](https://microsoft.github.io/language-server-protocol/specification/#uri)
   * that all servers work fine with URIs in both formats: `file:///C:/foo` and `file:///c%3A/foo`.
   *
   * VS Code always sends a lowercased Windows drive letter and always escapes colon
   * (see this [issue](https://github.com/microsoft/vscode-languageserver-node/issues/1280)
   * and the related [pull request](https://github.com/microsoft/language-server-protocol/pull/1786)).
   *
   * Some LSP servers support only the VS Code-friendly URI format (`file:///c%3A/foo`), so it's safer to use it by default.
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
   * Extracts a file path from [fileUri] and calls [findLocalFileByPath]. Respects only `file://...` URIs.
   * @param fileUri a [DocumentUri](https://microsoft.github.io/language-server-protocol/specification/#documentUri) received from the LSP
   *                server within some response or notification
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
    }
    catch (e: URISyntaxException) {
      LOG.warn("Malformed URI: " + fileUri + "; " + e.message)
      null
    }
  }

  /**
   * @see findFileByUri
   */
  protected open fun findLocalFileByPath(path: String): VirtualFile? = LocalFileSystem.getInstance().findFileByPath(path)

  /**
   * Returns a `languageId` field of the [TextDocumentItem](https://microsoft.github.io/language-server-protocol/specification#textDocumentItem) class,
   * which is needed for the [textDocument/didOpen](https://microsoft.github.io/language-server-protocol/specification#textDocumentItem)
   * notification. `languageId` is usually equal to the lowercased file extension. Standard implementation also handles some known
   * exceptions to this rule.
   */
  open fun getLanguageId(file: VirtualFile): String = Companion.getLanguageId(file)

  /**
   * [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams) object is sent to the LSP
   * server as [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) request.
   */
  open fun createInitializeParams(): InitializeParams = InitializeParams().apply {
    clientInfo = ClientInfo(ApplicationNamesInfo.getInstance().fullProductNameWithEdition,
                            ApplicationInfo.getInstance().build.asStringWithoutProductCode())

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
   * [ClientCapabilities](https://microsoft.github.io/language-server-protocol/specification/#clientCapabilities) is a mandatory field in
   * [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams) class, which is sent to the LSP
   * server as [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) request.
   *
   * Plugins may override this property and tune capabilities according to their needs.
   * For example, plugins may need to set [workspace.configuration][org.eclipse.lsp4j.WorkspaceClientCapabilities.setConfiguration]
   * capability to `true`, and provide required implementation in the overridden [getWorkspaceConfiguration] function.
   */
  open val clientCapabilities: ClientCapabilities
    get() = ClientCapabilities().apply {
      workspace = WorkspaceClientCapabilities().apply {
        workspaceFolders = true
        //configuration = true // keep false by default because [getWorkspaceConfiguration] returns null by default
        workspaceEdit = WorkspaceEditCapabilities().apply {
          documentChanges = true
          failureHandling = FailureHandlingKind.Abort
          resourceOperations = listOf(ResourceOperationKind.Create)
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
          completionList = CompletionListCapabilities().apply {
            itemDefaults = listOf("commitCharacters", "editRange", "insertTextFormat", "insertTextMode", "data")
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
          tagSupport = Either.forRight(DiagnosticsTagSupport(listOf(DiagnosticTag.Unnecessary, DiagnosticTag.Deprecated)))
        }
        codeAction = CodeActionCapabilities().apply {
          codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
            codeActionKind = CodeActionKindCapabilities(listOf(
              CodeActionKind.QuickFix,
              CodeActionKind.Empty,
              CodeActionKind.Source,
              CodeActionKind.Refactor,
            ))
          }
          disabledSupport = true
          dataSupport = true
          resolveSupport = CodeActionResolveSupportCapabilities().apply {
            properties = listOf("edit")
          }
        }
        formatting = FormattingCapabilities(true)
        references = ReferencesCapabilities(true)
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
   * `initializationOptions` is an optional field in the
   * [InitializeParams](https://microsoft.github.io/language-server-protocol/specification#initializeParams)
   * class, which is sent to the LSP server as
   * [initialize](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#initialize) request.
   */
  open fun createInitializationOptions(): Any? = null

  /**
   * Creates an implementation of the [org.eclipse.lsp4j.services.LanguageClient] interface.
   * It handles all standard requests and notifications that the LSP server sends to the IDE.
   * 'Standard' requests and notifications are the ones that are documented
   * in the official [LSP specification](https://microsoft.github.io/language-server-protocol/specification).
   *
   * To handle custom undocumented requests/notifications from the server, plugins may override this function
   * and return their subclass of the [Lsp4jClient] class.
   * See [Lsp4jClient] class documentation for more information.
   */
  open fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient = Lsp4jClient(handler)

  /**
   * Returns a class that should be used as a [Lsp4jServer] for this [LspServer].
   *
   * A plugin needs to define a custom sub-interface of the [Lsp4jServer] interface only if it needs to send
   * some custom undocumented notifications or requests to the LSP server.
   *
   * Note that there's no need to implement such a custom sub-interface.
   * The `lsp4j` library will generate the interface implementation using Java reflection ([Proxy][java.lang.reflect.Proxy])
   * based on `lsp4j`-specific annotations.
   *
   * Example:
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
   * Plugins may provide their listeners to get notified about [LspServer] events.
   */
  open val lspServerListener: LspServerListener? = null

  /**
   * Tells whether IntelliJ's `Navigate -> Declaration` action should use data from the LSP server by sending the
   * [textDocument/definition](https://microsoft.github.io/language-server-protocol/specification/#textDocument_definition) request.
   */
  open val lspGoToDefinitionSupport = true

  /**
   * Whether the IDE should ask the LSP server for hover information by sending the
   * [textDocument/hover](https://microsoft.github.io/language-server-protocol/specification/#textDocument_hover) request.
   */
  open val lspHoverSupport = true

  /**
   * By default, a standard [LspCompletionSupport] implementation is used.
   * Implementations may override this property and return their specific subclass of [LspCompletionSupport] that fine-tunes the
   * code completion behavior (for example, provides a special icon for each completion item). Also, implementations may return `null`
   * if there's no need in using built-in code completion support for this LSP server.
   */
  open val lspCompletionSupport: LspCompletionSupport? = LspCompletionSupport()

  /**
   * See LSP specification:
   * [Semantic Tokens](https://microsoft.github.io/language-server-protocol/specification/#textDocument_semanticTokens)
   *
   * Plugins may provide basic syntax highlighting using not only information from the LSP server but also other approaches,
   * such as TextMate bundles.
   *
   * - Implementations may return `null` to disable server-based semantic highlighting
   * - Implementations may override `LspSemanticTokensSupport` class to handle server-specific semantic tokens
   */
  open val lspSemanticTokensSupport: LspSemanticTokensSupport? = null //LspSemanticTokensSupport()

  /**
   * By default, a standard [LspDiagnosticsSupport] implementation is used for handling the
   * [textDocument/publishDiagnostics](https://microsoft.github.io/language-server-protocol/specification/#textDocument_publishDiagnostics)
   * notifications from the server. Implementations may override this property and return their specific subclass of [LspDiagnosticsSupport]
   * that fine-tunes the errors and warnings, as well as corresponding quick fixes. Also, implementations may return `null`
   * if there's no need in using built-in diagnostics support for this LSP server.
   */
  open val lspDiagnosticsSupport: LspDiagnosticsSupport? = LspDiagnosticsSupport()

  open val lspCodeActionsSupport: LspCodeActionsSupport? = LspCodeActionsSupport()

  /**
   * Handles [Command](https://microsoft.github.io/language-server-protocol/specification#command) objects received from the LSP server.
   * @see LspCommandsSupport.executeCommand
   */
  open val lspCommandsSupport: LspCommandsSupport? = LspCommandsSupport()

  /**
   * Helps to decide whether the LSP server should be used for code formatting for each particular file.
   * Implementations may return `null` to disable server-based code formatting completely.
   */
  open val lspFormattingSupport: LspFormattingSupport? = LspFormattingSupport()

  /**
   * If not `null`, then the `Find Usages` and `Show Usages` features will use the
   * [textDocument/references](https://microsoft.github.io/language-server-protocol/specification/#textDocument_references)
   * request to get results.
   */
  open val lspFindReferencesSupport: FindReferencesSupport? = FindReferencesSupport()

  /**
   * Handles
   * [workspace/configuration](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#workspace_configuration)
   * request from the server.
   *
   * Plugins that override this function should probably also override [clientCapabilities] property,
   * and set [workspace.configuration][org.eclipse.lsp4j.WorkspaceClientCapabilities.setConfiguration] capability to `true`.
   */
  open fun getWorkspaceConfiguration(item: ConfigurationItem): Any? = null

  companion object {
    @JvmField
    val LOG: Logger = Logger.getInstance(LspServerDescriptor::class.java)

    fun getLanguageId(file: VirtualFile): String {
      val nameLowercased = StringUtil.toLowerCase(file.name)
      FILE_NAME_ENDING_TO_LANGUAGE_ID.find { nameLowercased.endsWith(it.first) }?.let { return it.second }
      return StringUtil.toLowerCase(file.extension)?.let { FILE_EXTENSION_TO_LANGUAGE_ID[it] ?: it } ?: ""
    }

    private val FILE_NAME_ENDING_TO_LANGUAGE_ID: List<Pair<String, String>> = listOf(
      ".blade.php" to "blade"
    )

    // https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocumentItem
    // Language ID is usually equal to the lowercased file extension.
    // This map tracks only non-standard cases.
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
