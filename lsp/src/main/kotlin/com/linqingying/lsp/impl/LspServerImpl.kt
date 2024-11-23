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

package com.linqingying.lsp.impl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.text.Strings.nullize
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.linqingying.lsp.api.*
import com.linqingying.lsp.impl.connector.Lsp4jServerConnector
import com.linqingying.lsp.impl.connector.Lsp4jServerConnectorSocket
import com.linqingying.lsp.impl.connector.Lsp4jServerConnectorStdio
import com.linqingying.lsp.impl.connector.LspInitializationException
import com.linqingying.lsp.impl.highlighting.DiagnosticAndQuickFixes
import com.linqingying.lsp.impl.highlighting.LspDiagnosticsCache
import com.linqingying.lsp.impl.highlighting.LspSemanticToken
import com.linqingying.lsp.impl.highlighting.LspSemanticTokensCache
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageServer
import org.jetbrains.annotations.NonNls
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource


class LspServerImpl(
    override val providerClass: Class<out LspServerSupportProvider>,
    override val descriptor: LspServerDescriptor,
    private val eventBroadcaster: LspServerManagerListener

) : LspServer {
    companion object {
        private val LOG = Logger.getInstance(
            LspServerImpl::class.java
        )
    }

    internal class FileChangeInfo(
        path: String,
        val uri: String,
        isDirectory: Boolean,
        val changeType: FileChangeType
    ) : FileInfo(path, isDirectory) {


        fun doesFileWatcherKindMatchFileChangeType(watchKind: Int?): Boolean {
            if (watchKind == null) {
                return true
            }

            return when (changeType) {
                FileChangeType.Created -> (watchKind and 1) != 0
                FileChangeType.Changed -> (watchKind and 2) != 0
                FileChangeType.Deleted -> (watchKind and 4) != 0
                else -> throw IllegalArgumentException("Unknown change type: $changeType")
            }
        }
    }

    internal val serverNotificationsHandler: LspServerNotificationsHandler = LspServerNotificationsHandlerImpl(
        this
    )
    internal val dynamicCapabilities: LspDynamicCapabilities = LspDynamicCapabilities()
    internal val errorOutput: String? get() = nullize(errorOutputBuffer.toString(), false)
    val openedFiles: MutableSet<VirtualFile> = Collections.synchronizedSet(HashSet())
    private val unsupportedFilePaths: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val stateLock: Any = Any()
    internal val textDocumentSyncKind: TextDocumentSyncKind?
        get() {
            val serverCapabilities = serverCapabilities
            return serverCapabilities?.textDocumentSync?.let { either ->
                when {
                    either.isLeft -> either.left
                    else -> (either.right as TextDocumentSyncOptions).change
                }
            }
        }

    private var lsp4jServerConnector: Lsp4jServerConnector? = null
    private val connectorLock: Any = Any()
    override val project: Project
        get() = descriptor.project
    override var state: LspServerState = LspServerState.Initializing
    internal val serverCapabilities: ServerCapabilities?
        get() {

            return if (this.state === LspServerState.Running) {

                initializeResult?.capabilities
            } else {
                null
            }


        }
    override var initializeResult: InitializeResult? = null
    private val errorOutputBuffer: StringBuilder = StringBuilder()
    private val semanticTokensCache = LspSemanticTokensCache(this)
    private val diagnosticsCache: LspDiagnosticsCache = LspDiagnosticsCache()
    internal fun supportsFindReferences(file: VirtualFile): Boolean {
        val serverCapabilities = serverCapabilities
        serverCapabilities?.referencesProvider?.let { referencesProvider ->
            val leftValue = referencesProvider.left
            return leftValue ?: true
        }
        return checkDynamicCapabilities(file, LspDynamicCapabilities.references)
    }

    private fun isRunning(): Boolean {
        return state == LspServerState.Running
    }

    internal fun sendOpenedFiles() {
        ReadAction.nonBlocking<Set<VirtualFile>> {
            val newFiles = mutableSetOf<VirtualFile>()
            val openFiles = FileEditorManager.getInstance(project).openFiles
            for (file in openFiles) {
                if (!openedFiles.contains(file) && isSupportedFile(file)) {
                    newFiles.add(file)
                }
            }
            val unsavedDocuments = FileDocumentManager.getInstance().unsavedDocuments
            for (document in unsavedDocuments) {
                val file = FileDocumentManager.getInstance().getFile(document)
                if (file != null && !openedFiles.contains(file) && isSupportedFile(file)) {
                    newFiles.add(file)
                }
            }
            newFiles
        }
            .expireWhen {
                !isRunning()
            }
            .finishOnUiThread(ModalityState.nonModal()) { files ->
                if (files.isNotEmpty()) {
                    WriteAction.run<Throwable> {
                        LOG.debug("Opening files after server initialization or after move/rename: $files")
                        files.forEach { sendDidOpenRequest(it) }
                    }
                }

            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    @RequiresWriteLock
    internal fun sendDidOpenRequest(file: VirtualFile) {
        if (this.state != LspServerState.Running) {
            logError("Server is not in the Running state. Ignoring sendDidOpenRequest($file)")
        } else {
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document == null) {
                logInfo("Skipping didOpen request because there's no document for file $file")
            } else {
                if (openedFiles.add(file)) {
                    val textDocumentItem = TextDocumentItem(
                        descriptor.getFileUri(file),
                        descriptor.getLanguageId(file),
                        getDocumentVersion(document),
                        document.text
                    )
                    sendNotification { it: LanguageServer ->

                        it.textDocumentService.didOpen(DidOpenTextDocumentParams(textDocumentItem))
                    }
                    eventBroadcaster.fileOpened(this, file)
                } else {
                    logError("sendDidOpenRequest() cannot be called for already opened files. Ignoring: $file")
                }
            }
        }
    }

    private fun getFileEvent(
        fileChangeInfo: FileChangeInfo,
        registrationOptions: List<DidChangeWatchedFilesRegistrationOptions>
    ): FileEvent? {
        for (registration in registrationOptions) {
            for (watcher in registration.watchers) {
                if (!fileChangeInfo.doesFileWatcherKindMatchFileChangeType(watcher.kind)) {
                    continue
                }

                val patternResult = watcher.globPattern
                val uri: String = when {
                    patternResult.isLeft -> patternResult.left
                    else -> {
                        val relativePattern = patternResult.right
                        val baseUri = relativePattern.baseUri
                        val workspaceFolderUri = baseUri?.left?.uri ?: baseUri?.right ?: return null

                        val fileDescriptor = this.descriptor
                        val virtualFile = fileDescriptor.findFileByUri(workspaceFolderUri)

                        if (virtualFile != null && virtualFile.isDirectory) {
                            val filePattern = relativePattern.pattern
                            if (this.checkFileInfo(fileChangeInfo, filePattern, virtualFile.path)) {
                                return FileEvent(fileChangeInfo.uri, fileChangeInfo.changeType)
                            }
                        }
                        return null
                    }
                }

                if (this.checkFileInfo(fileChangeInfo, uri, null)) {
                    return FileEvent(fileChangeInfo.uri, fileChangeInfo.changeType)
                }
            }
        }
        return null
    }

    internal fun processFileEvents(fileChangeInfos: Collection<FileChangeInfo>) {
        val registrationOptions =
            this.dynamicCapabilities.getCapabilityRegistrationOptions(LspDynamicCapabilities.didChangeWatchedFiles)
        val fileEvents = mutableListOf<FileEvent>()

        for (fileChangeInfo in fileChangeInfos) {
            val fileEvent = this.getFileEvent(fileChangeInfo, registrationOptions)
            fileEvent?.let { fileEvents.add(it) }
        }

        if (fileEvents.isNotEmpty()) {
            this.sendNotification { languageServer ->

                languageServer.workspaceService.didChangeWatchedFiles(DidChangeWatchedFilesParams(fileEvents))
            }
        }
    }
    private fun updateServerState(newState: LspServerState) {
        this.state = newState
        this.eventBroadcaster.serverStateChanged(this)
    }
    /**
     * 清理，关闭并退出
     */
    fun cleanupShutdownAndExit(shutdownNormally: Boolean) {
        if (this.state !== LspServerState.Initializing && this.state !== LspServerState.Running) {
            LOG.debug("Attempt to stop server in wrong myState")
        } else {
            LOG.debug("Stopping server")
            this.updateServerState(if (shutdownNormally) LspServerState.ShutdownNormally else LspServerState.ShutdownUnexpectedly)

            openedFiles.clear()
            requestExecutor.shutdownNow()
            val task = Runnable {
                synchronized(connectorLock) {
                    lsp4jServerConnector?.shutdownExitDisconnect()
                }
            }
            if (!ApplicationManager.getApplication().isDispatchThread && !ApplicationManager.getApplication().isReadAccessAllowed) {
                task.run()
            } else {
                ApplicationManager.getApplication().executeOnPooledThread(task)
            }
        }
    }
    internal fun diagnosticsReceived(params: PublishDiagnosticsParams) {

        val uri = params.uri

        val virtualFile = descriptor.findFileByUri(uri)

        if (virtualFile != null && params.version != null && this.openedFiles.contains(virtualFile)) {
            val document = FileDocumentManager.getInstance().getCachedDocument(virtualFile)
            if (document != null) {
                val currentVersion = this.getDocumentVersion(document)
                val receivedVersion = params.version

                if (receivedVersion != null && currentVersion != receivedVersion) {
                    this.logDebug("Ignoring diagnostics (version $receivedVersion) for ${virtualFile.name}; current document version: $currentVersion")
                    return
                }
            }
        }

        this.diagnosticsCache.diagnosticsReceived(virtualFile, params)

        if (virtualFile != null) {
            LspServerManagerImpl.getInstanceImpl(this.project).onDiagnosticsReceived(this, virtualFile)
        }
    }

    @RequiresBackgroundThread
    @RequiresReadLock
    internal fun getFilesToClose(): Collection<VirtualFile> {
        val editorManager = FileEditorManager.getInstance(project)
        val documentManager = FileDocumentManager.getInstance()
        val openedFilesSet = this.openedFiles
        val filesToClose = mutableListOf<VirtualFile>()

        for (virtualFile in openedFilesSet) {
            if (!editorManager.isFileOpen(virtualFile)) {
                val cachedDocument = documentManager.getCachedDocument(virtualFile)
                if (cachedDocument == null || !documentManager.isDocumentUnsaved(cachedDocument)) {
                    filesToClose.add(virtualFile)
                }
            }
        }

        return filesToClose
    }

    private fun createConnector(): Lsp4jServerConnector {
        return when (val communicationChannel = this.descriptor.lspCommunicationChannel) {
            is LspCommunicationChannel.StdIO -> Lsp4jServerConnectorStdio(this)
            is LspCommunicationChannel.Socket -> Lsp4jServerConnectorSocket(this)
            else -> throw RuntimeException("Unexpected communication channel: $communicationChannel")
        }
    }

    private fun initializeServer(
        startTime:
        Duration
    ) {

        try {
            synchronized(connectorLock) {
                lsp4jServerConnector = createConnector()
                val connector =
                    lsp4jServerConnector ?: throw UninitializedPropertyAccessException("lsp4jServerConnector")

                connector.connect { initializeResult ->

                    this.initializeResult = initializeResult

                    synchronized(stateLock) {
                        if (state == LspServerState.Initializing) {

                            state = LspServerState.Running
                        }
                    }

                    val elapsedTime = TimeSource.Monotonic.markNow().elapsedNow() - startTime
                    val initializationMessage =
                        "LSP server initialized in ${elapsedTime.toString(DurationUnit.SECONDS, 3)}"

                    val serverInfo = initializeResult.serverInfo
                    val infoMessage = serverInfo?.let {
                        "$initializationMessage, name = ${it.name}, version = ${it.version}"
                    } ?: initializationMessage

                    logInfo(infoMessage)

                    descriptor.lspServerListener?.serverInitialized(initializeResult)
                }
            }

            sendOpenedFiles()
        } catch (e: Exception) {
            val cause = (e as? LspInitializationException)?.cause ?: e
            logWarn("Failed to start LSP server", cause)

            val manager = ReadAction.compute<LspServerManagerImpl, Throwable> {
                if (!project.isDisposed) {
                    LspServerManagerImpl.getInstanceImpl(project)
                }
                null
            }
            val errorMessage = if (e is LspInitializationException) "$e\nCaused by:\n" else ""
            val stackTrace = errorMessage + cause.stackTraceToString()

            manager?.handleMaybeUnexpectedServerStop(this, stackTrace)
        }
    }

    internal fun start() {
        if (state != LspServerState.Initializing) {
            logError("start() cannot be called for a server twice")
        } else {
            logInfo("Starting LSP server")
            val startTime = TimeSource.Monotonic.markNow()
            ApplicationManager.getApplication().executeOnPooledThread {
                initializeServer(
                    startTime.elapsedNow()
                )
            }
        }
    }

    private fun <T : TextDocumentRegistrationOptions> checkDynamicCapabilities(
        file: VirtualFile,
        capabilityAndOptionsClass: Pair<String, Class<T>>
    ): Boolean {
        if (file.isDirectory) {
            logWarn("Directory not expected here. Capability: ${capabilityAndOptionsClass.first}, file: ${file.path}")
            return false
        }

        val options = dynamicCapabilities.getCapabilityRegistrationOptions(capabilityAndOptionsClass)

        for (registrationOptions in options) {
            val documentSelector = registrationOptions.documentSelector ?: continue

            for (filter in documentSelector) {
                if (filter.scheme == "file") {
                    val language = filter.language
                    val pattern = filter.pattern

                    if (language == null && pattern == null) continue

                    if (language != null && language != descriptor.getLanguageId(file)) continue

                    if (pattern == null) return true

                    val filePath = file.path
                    if (this.checkFileInfo(FileInfo(filePath, false), pattern, null)) return true
                }
            }
        }

        return false
    }

    private fun checkFileInfo(
        fileInfo: FileInfo,
        globPattern: String,
        basePath: String?
    ): Boolean {
        val path = when {
            basePath == null -> fileInfo.path
            fileInfo.path == basePath -> ""
            fileInfo.path.startsWith("$basePath/") -> fileInfo.path.substring(basePath.length + 1)

            else -> return false
        }

        return if (fileInfo.isDirectory) {
            true
        } else {
            val pathMatcher = dynamicCapabilities.getPathMatcherCaching(globPattern)
            pathMatcher.matches(Path.of(path))
        }
    }

    open class FileInfo(
        val path: String,
        val isDirectory: Boolean
    )

    override fun sendNotification(lsp4jSender: (Lsp4jServer) -> Unit) {
        requestExecutor.sendNotification(lsp4jSender)

    }

    @RequiresWriteLock
    internal fun sendDidCloseRequest(file: VirtualFile) {
        if (!openedFiles.remove(file)) {
            logError("sendDidCloseRequest() cannot be called for files that haven't been opened. Ignoring: $file")
        } else {
            val params = DidCloseTextDocumentParams(getDocumentIdentifier(file))
            sendNotification { server ->
                server.textDocumentService.didClose(params)
            }
        }
    }

    internal fun appendServerErrorOutput(text: String) {
        if (errorOutputBuffer.isNotEmpty()) {
            errorOutputBuffer.append("\n")
        }

        when {
            text.length > 1_048_576 -> {
                errorOutputBuffer.replace(0, errorOutputBuffer.length, text.takeLast(1_048_576))
            }

            errorOutputBuffer.length + text.length > 1_048_576 -> {
                errorOutputBuffer.delete(0, errorOutputBuffer.length + text.length - 1_048_576)
                errorOutputBuffer.append(text)
            }

            else -> {
                errorOutputBuffer.append(text)
            }
        }
    }

    @RequiresBackgroundThread
    internal fun getSemanticTokens(file: VirtualFile): List<LspSemanticToken> {
        return semanticTokensCache.getSemanticTokens(file)

    }

    internal fun ensureServerStopped(
        explicitStop: Boolean,
        updateLspServerManagerState: () -> Unit
    ) {
        fun updateServerState(newState: LspServerState) {
            if (newState != LspServerState.Initializing &&
                (state == LspServerState.Initializing || newState != LspServerState.Running) &&
                state != LspServerState.ShutdownNormally &&
                state != LspServerState.ShutdownUnexpectedly
            ) {

                state = newState
                eventBroadcaster.serverStateChanged(this)
            } else {
                logger.error("Incorrect state change: $state -> $newState")
            }
        }
        synchronized<Any>(stateLock) {
            try {
                updateLspServerManagerState()

                if (state in arrayOf(LspServerState.ShutdownNormally, LspServerState.ShutdownUnexpectedly)) {
                    return
                }

                logInfo("Stopping LSP server ${if (explicitStop) "normally" else "unexpectedly"}")
                updateServerState(if (explicitStop) LspServerState.ShutdownNormally else LspServerState.ShutdownUnexpectedly)

                openedFiles.clear()
                requestExecutor.shutdownNow()
                semanticTokensCache.clearCache()
                diagnosticsCache.clearCache()
            } finally {
                // No-op in Kotlin, can be omitted if no cleanup is needed
            }
        }

        executeTask()
    }

    private fun createServerConnector(): Lsp4jServerConnector {
        return when (  descriptor.lspCommunicationChannel) {
            is LspCommunicationChannel.StdIO -> Lsp4jServerConnectorStdio(this)
            is LspCommunicationChannel.Socket -> Lsp4jServerConnectorSocket(this)
        }
    }

    @RequiresBackgroundThread
    fun getDiagnosticsAndQuickFixes(file: VirtualFile): List<DiagnosticAndQuickFixes> {
        val diagnostics = diagnosticsCache.getDiagnostics(file)
        val quickFixesList = mutableListOf<DiagnosticAndQuickFixes>()

        for (diagnosticAndQuickFixes in diagnostics) {
            val diagnostic = diagnosticAndQuickFixes.diagnostic
            val quickFixes = diagnosticAndQuickFixes.getQuickFixes(this, file)
            quickFixesList.add(DiagnosticAndQuickFixes(diagnostic, quickFixes))
        }

        return quickFixesList
    }


    internal fun fileEdited(
        file: VirtualFile,
        e: DocumentEvent
    ) {
        semanticTokensCache.fileEdited(file, e)
        diagnosticsCache.fileEdited(file, e)
    }

    internal fun supportsGotoDefinition(): Boolean {
        val serverCapabilities = serverCapabilities
        return if (serverCapabilities != null) {
            val definitionProvider = serverCapabilities.definitionProvider
            if (definitionProvider != null) {
                val isSupported = definitionProvider.left
                isSupported ?: true
            } else {
                false
            }
        } else {
            false
        }
    }

    internal fun hasFormattingRelatedCapabilities(): Boolean {
        if (dynamicCapabilities.hasCapability(LspDynamicCapabilities.formatting)) {
            return true
        }

        val serverCapabilities = serverCapabilities
        val documentFormattingProvider = serverCapabilities?.documentFormattingProvider

        return when {
            documentFormattingProvider == null -> false
            documentFormattingProvider.isLeft -> documentFormattingProvider.left ?: true
            else -> true
        }
    }

    internal fun supportsHover(): Boolean {
        val hoverProvider = serverCapabilities?.hoverProvider

        return when {
            hoverProvider == null -> false
            hoverProvider.isLeft -> hoverProvider.left ?: true
            else -> true
        }
    }

    internal fun doesServerExplicitlyWantToFormatThisFile(file: VirtualFile): Boolean {
        return checkDynamicCapabilities(file, LspDynamicCapabilities.formatting)
    }

    private fun executeTask() {
        val task = Runnable {
            synchronized(connectorLock) {
                lsp4jServerConnector?.shutdownExitDisconnect() ?: run {
                    throw UninitializedPropertyAccessException("lsp4jServerConnector")
                }

            }
        }
        if (!ApplicationManager.getApplication().isDispatchThread && !ApplicationManager.getApplication().isReadAccessAllowed) {
            task.run()
        } else {
            ApplicationManager.getApplication().executeOnPooledThread(task)
        }
    }

    private fun generateMessage(message: String): String {
        return "${this.javaClass.simpleName}: $message"
    }


    internal fun isFileOpened(file: VirtualFile): Boolean {
        return openedFiles.contains(file)
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    internal fun isSupportedFile(file: VirtualFile): Boolean {
        if (!file.isInLocalFileSystem) return false
        if (unsupportedFilePaths.contains(file.path)) return false



            if (!ProjectFileIndex.getInstance(project).isInContent(file)) return false


        val isSupported = descriptor.isSupportedFile(file)
        if (!isSupported) {
            unsupportedFilePaths.add(file.path)
        }

        return isSupported
    }


    fun logDebug(@NonNls message: String) {


        LOG.debug(this.generateMessage(message))
    }

    fun logError(@NonNls message: String) {


        LOG.error(this.generateMessage(message))
    }

    fun logWarn(@NonNls message: String, t: Throwable? = null) {


        LOG.warn(this.generateMessage(message), t)
    }

    fun logInfo(@NonNls message: String) {
        LOG.info(this.generateMessage(message))
    }

    override suspend fun <Lsp4jResponse> sendRequest(lsp4jSender: (Lsp4jServer) -> CompletableFuture<Lsp4jResponse>): Lsp4jResponse? {
        return requestExecutor.sendRequest(lsp4jSender)
    }

    override fun <Lsp4jResponse> sendRequestSync(
        timeoutMs: Int,
        lsp4jSender: (Lsp4jServer) -> CompletableFuture<Lsp4jResponse>
    ): Lsp4jResponse? {
        return requestExecutor.sendRequestSync(timeoutMs, lsp4jSender)
    }

    override fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier {
        return TextDocumentIdentifier(descriptor.getFileUri(file))
    }

    override fun getDocumentVersion(document: Document): Int {
        return if (document is DocumentEx) {
            document.modificationSequence
        } else {
            document.modificationStamp.toInt()
        }
    }

    override val lsp4jServer: Lsp4jServer
        get() = lsp4jServerConnector.let { it?.lsp4jServer } ?: throw IllegalStateException("Server is not running")
    override val requestExecutor: LspRequestExecutorImpl = LspRequestExecutorImpl(this)
}
