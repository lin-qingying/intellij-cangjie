package com.linqingying.lsp.impl

import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.linqingying.lsp.api.*
import com.linqingying.lsp.api.customization.requests.LspRequestExecutor
import com.linqingying.lsp.impl.connector.Lsp4jServerConnector
import com.linqingying.lsp.impl.connector.Lsp4jServerConnectorStdio
import com.linqingying.lsp.impl.highlighting.DiagnosticAndQuickFixes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.util.containers.ContainerUtil
import com.linqingying.lsp.impl.requests.*
import com.linqingying.lsp.impl.requests.DidOpenNotification
import org.eclipse.lsp4j.*

import org.eclipse.lsp4j.services.LanguageServer
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicReference


class LspServerImpl(
    val pluginClass: Class<out LspServerSupportProvider>,
    override val descriptor: LspServerDescriptor,
    private val listenersAdapter: LspServerManagerListener

) : LspServer {

    enum class State {
        CREATED,
        RUNNING,
        STOPPED,
        MALFUNCTIONED
    }


    private val state: AtomicReference<State> = AtomicReference(State.CREATED)


    internal val dynamicCapabilities: LspDynamicCapabilities = LspDynamicCapabilities()


    val openedFiles: MutableSet<VirtualFile> = Collections.synchronizedSet(HashSet())

    private val unsupportedFilePaths: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val diagnosticsCache: LspDiagnosticsCache = LspDiagnosticsCache()


    private var lsp4jServerConnector: Lsp4jServerConnector? = null
    private val connectorLock: Any = Any()

    private val myRequestExecutor = LspRequestExecutorImpl(this)
    private val myServerNotificationsHandler: LspServerNotificationsHandler = LspServerNotificationsHandlerImpl(this)

    @RequiresWriteLock
    fun sendDidCloseRequest(file: VirtualFile) {


        ApplicationManager.getApplication().assertWriteAccessAllowed()
        if (!openedFiles.remove(file)) {
            Log.error("sendDidCloseRequest() cannot be called for files that haven't been opened. Ignoring: $file")
        } else {
            requestExecutor.sendNotification(DidCloseNotification(this, file))
        }
    }

    fun start() {
        if (!state.compareAndSet(State.CREATED, State.RUNNING)) {
            LOG.debug("Attempt to start server in wrong myState")
        } else {
            LOG.info("Starting server")
            openedFiles.clear()
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    synchronized(connectorLock) {
                        val connector = Lsp4jServerConnectorStdio(this)
                        lsp4jServerConnector = connector
                        connector.connect()
                    }
                    sendOpenedFiles()
                } catch (e: Exception) {
                    LOG.warn("Failed to start server", e)
                    listenersAdapter.serverInitializationFailed()
                    cleanupShutdownAndExit(true)
                }
            }
        }
    }

    fun isMalfunctioned(): Boolean = state.get() == State.MALFUNCTIONED
    fun diagnosticsReceived(params: PublishDiagnosticsParams) {
        val file = descriptor.findFileByUri(params.uri)
        if (file != null && params.version != null && this.openedFiles.contains(file)) {
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            if (document != null && requestExecutor.getDocumentVersion(document) != params.version) {
                LOG.debug(
                    "Ignoring diagnostics (version ${params.version}) for ${file.name}; current document version: ${
                        requestExecutor.getDocumentVersion(
                            document
                        )
                    }"
                )
                return
            }
        }

        this.diagnosticsCache.diagnosticsReceived(file, params)
        if (file != null) {
            LspServerManagerImpl.getInstanceImpl(project).onDiagnosticsReceived(file)
        }
    }

    /**
     * 清理，关闭并退出
     */
    fun cleanupShutdownAndExit(malfunction: Boolean) {
        if (!state.compareAndSet(State.RUNNING, if (malfunction) State.MALFUNCTIONED else State.STOPPED)) {
            LOG.debug("Attempt to stop server in wrong myState")
        } else {
            LOG.debug("Stopping server")
            openedFiles.clear()
            (requestExecutor as LspRequestExecutorImpl).shutdownNow()
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

    override val project: Project
        get() = descriptor.project

    override val lsp4jServer: LanguageServer
        get() = lsp4jServerConnector.let { it?.lsp4jServer } ?: throw IllegalStateException("Server is not running")
    override val requestExecutor: LspRequestExecutor
        get() = myRequestExecutor
    override val serverNotificationsHandler: LspServerNotificationsHandler
        get() = myServerNotificationsHandler


    fun isRunning(): Boolean {
        return state.get() == State.RUNNING
    }

    /**
     * 处理文件变化事件
     */
    fun processFileEvents(fileChangeInfos: Collection<FileChangeInfo>) {


        val registrationOptions =
            dynamicCapabilities.getCapabilityRegistrationOptions(LspDynamicCapabilities.didChangeWatchedFiles)
        val roots = descriptor.roots.map { descriptor.getFileUri(it) }.toTypedArray()
        val notifications = fileChangeInfos.map { processFileEvent(it, registrationOptions, roots) }

        if (notifications.isNotEmpty()) {
            requestExecutor.sendNotification(DidChangeWatchedFilesNotification(this, notifications))
        }
    }

    private fun matchGlobPattern(fileChangeInfo: FileChangeInfo, globPattern: String, roots: Array<String>): Boolean {


        for (root in roots) {
            val relativePath = when {
                fileChangeInfo.uri == root -> ""
                fileChangeInfo.uri.startsWith("$root/") -> fileChangeInfo.uri.substring(root.length + 1)
                else -> continue
            }

            if (fileChangeInfo.isDirectory) {
                return true
            }

            val pathMatcher = dynamicCapabilities.getPathMatcherCaching(globPattern)
            if (pathMatcher.matches(Paths.get(relativePath))) {
                return true
            }
        }

        return false
    }

    private fun processFileEvent(
        fileChangeInfo: FileChangeInfo,
        registrationOptions: List<DidChangeWatchedFilesRegistrationOptions>,
        roots: Array<String>
    ): FileEvent? {

        for (option in registrationOptions) {
            for (watcher in option.watchers) {
                if (watcher.kind != null && watcher.kind and fileChangeInfo.changeType.value == 0) {
                    continue
                }

                if (watcher.globPattern.isLeft) {
                    val globPattern = watcher.globPattern.left
                    if (matchGlobPattern(fileChangeInfo, globPattern, roots)) {
                        return FileEvent(fileChangeInfo.uri, fileChangeInfo.changeType)
                    }
                } else {
                    val relativePattern = watcher.globPattern.right
                    val baseUri =
                        if (relativePattern.baseUri.isLeft) relativePattern.baseUri.left.uri else relativePattern.baseUri.right
                    val pattern = relativePattern.pattern
                    if (matchGlobPattern(fileChangeInfo, pattern, arrayOf(baseUri))) {
                        return FileEvent(fileChangeInfo.uri, fileChangeInfo.changeType)
                    }
                }
            }
        }

        return null
    }

    fun fileEdited(file: VirtualFile, e: DocumentEvent) {

        diagnosticsCache.fileEdited(file, e)
    }

    private fun getTextDocumentSyncKind(): TextDocumentSyncKind? {
        val serverCapabilities = this.getServerCapabilities() ?: return null
        val textDocumentSync = serverCapabilities.textDocumentSync
        return if (textDocumentSync.isLeft) {
            textDocumentSync.left
        } else {
            (textDocumentSync.right as TextDocumentSyncOptions).change
        }
    }

    fun requiresFullSync(): Boolean {
        return getTextDocumentSyncKind() == TextDocumentSyncKind.Full
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getFilesToClose(): Collection<VirtualFile> {
        ApplicationManager.getApplication().assertIsNonDispatchThread()
        return if (openedFiles.isEmpty()) {
            emptyList()
        } else {
            val fileDocumentManager = FileDocumentManager.getInstance()
            val fileEditorManager = FileEditorManager.getInstance(project)
            openedFiles.filter { file ->
                ProgressManager.checkCanceled()
                if (fileEditorManager.isFileOpen(file)) {
                    false
                } else {
                    val document = fileDocumentManager.getCachedDocument(file)
                    document == null || !fileDocumentManager.isDocumentUnsaved(document)
                }
            }
        }
    }

    @RequiresBackgroundThread

    fun getDiagnosticsAndQuickFixes(file: VirtualFile): List<DiagnosticAndQuickFixes> {

        ApplicationManager.getApplication().assertIsNonDispatchThread()
        val diagnosticsAndQuickFixes = ContainerUtil.map(diagnosticsCache.getDiagnostics(file)) {
            DiagnosticAndQuickFixes(it.diagnostic, it.getQuickFixes(this, file))
        }

        return diagnosticsAndQuickFixes
    }

    fun requiresIncrementalSync(): Boolean {
        return getTextDocumentSyncKind() == TextDocumentSyncKind.Incremental
    }

    fun isFileOpened(file: VirtualFile): Boolean {

        return this.openedFiles.contains(file)
    }

    /**
     * 发送所有已打开且未保存的文件
     */
    fun sendOpenedFiles() {
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
        }.expireWhen { !isRunning() }
            .finishOnUiThread(ModalityState.nonModal()) { files ->
                if (files.isNotEmpty()) {
                    WriteAction.run<Throwable> {
                        LOG.debug("Opening files after server initialization or after move/rename: $files")
                        files.forEach { sendDidOpenRequest(it) }
                    }
                }
            }.submit(AppExecutorUtil.getAppExecutorService())
    }

    fun hasCapabilities(): Boolean {
        return this.getServerCapabilities() != null
    }

    fun getServerCapabilities(): ServerCapabilities? {
        val connector = lsp4jServerConnector
        return if (isRunning() && connector != null) connector.getServerCapabilities() else null
    }

    @RequiresWriteLock
    fun sendDidOpenRequest(file: VirtualFile) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()
        if (!hasCapabilities()) {
            LOG.error("sendDidOpenRequest() can only be called when hasCapabilities() == true. Ignoring $file")
        } else {
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document == null) {
                LOG.info("Skipping didOpen request because there's no document for file $file")
            } else {
                if (openedFiles.add(file)) {
                    requestExecutor.sendNotification(DidOpenNotification(this, file, document))
                    listenersAdapter.fileOpened(file)
                } else {
                    LOG.error("sendDidOpenRequest() cannot be called for already opened files. Ignoring: $file")
                }
            }
        }
    }

    //    semanticTokens/full
//    @RequiresWriteLock
//    fun sendSemanticTokensFullRequest(file: VirtualFile) {
//        ApplicationManager.getApplication().assertWriteAccessAllowed()
//        if (!hasCapabilities()) {
//            LOG.error("sendSemanticTokensFullRequest() can only be called when hasCapabilities() == true. Ignoring $file")
//        } else {
//
//            if (openedFiles.add(file)) {
//                requestExecutor.sendNotification(SemanticTokensFullNotification(this, file))
//                listenersAdapter.fileOpened(file)
//            } else {
//                LOG.error("sendSemanticTokensFullRequest() cannot be called for already opened files. Ignoring: $file")
//            }
//
//        }
//
//    }


    @RequiresReadLock
    @RequiresBackgroundThread
    fun isSupportedFile(file: VirtualFile): Boolean {
        ApplicationManager.getApplication().assertIsNonDispatchThread()
        if (!file.isInLocalFileSystem || unsupportedFilePaths.contains(file.path) || !ProjectFileIndex.getInstance(
                project
            ).isInContent(file)
        ) {
            return false
        }
        val isSupported = descriptor.isSupportedFile(file)
        if (!isSupported) {
            unsupportedFilePaths.add(file.path)
        }
        return isSupported
    }

    companion object {
        private val LOG = Logger.getInstance(
            LspServerImpl::class.java
        )
    }


    data class FileChangeInfo(
        val uri: String,
        val isDirectory: Boolean,
        val changeType: FileChangeType
    )


}
