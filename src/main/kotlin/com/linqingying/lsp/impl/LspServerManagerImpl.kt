package com.linqingying.lsp.impl

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.AsyncFileListener.ChangeApplier
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiManager
import com.intellij.util.EventDispatcher
import com.intellij.util.SmartList
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.linqingying.lsp.api.*
import com.linqingying.lsp.impl.requests.DidChangeNotification
import org.eclipse.lsp4j.FileChangeType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

class LspServerManagerImpl(val project: Project) : LspServerManager, Disposable {

    private val servers: MutableCollection<LspServerImpl> = ContainerUtil.createLockFreeCopyOnWriteList()

    private val listenersAdapter: LspServerManagerListener
    private val eventDispatcher: EventDispatcher<LspServerManagerListener> =
        EventDispatcher.create(LspServerManagerListener::class.java)


    private val highlightingQueue: MergingUpdateQueue =
        MergingUpdateQueue("LSP highlighting queue", 100, true, null as JComponent?, this, null as JComponent?, true)

    init {
        this.listenersAdapter = object : LspServerManagerListener {
            override fun serverInitializationFailed() {
                this@LspServerManagerImpl.eventDispatcher.getMulticaster().serverInitializationFailed()
            }

            override fun fileOpened(file: VirtualFile) {

                this@LspServerManagerImpl.eventDispatcher.getMulticaster().fileOpened(file)
            }

            override fun diagnosticsReceived(file: VirtualFile) {

                this@LspServerManagerImpl.eventDispatcher.getMulticaster().diagnosticsReceived(
                    file
                )
            }
        }
    }

    override fun getServersForProvider(providerClass: Class<out LspServerSupportProvider>): Collection<LspServer> {

        return this.servers.filter { it.pluginClass == providerClass }
    }

    override fun startServersIfNeeded(providerClass: Class<out LspServerSupportProvider>) {
        val provider = LspServerSupportProvider.EP_NAME.findExtension(providerClass)
        if (provider == null) {
            LOG.error("${providerClass.name} is not loaded")
        } else {
            ReadAction.nonBlocking<SmartList<LspServerDescriptor>> {
                val servers = getServersForProvider(providerClass)
                val descriptors = SmartList<LspServerDescriptor>()
                val openFiles = FileEditorManager.getInstance(project).openFiles
                for (file in openFiles) {
                    ProgressManager.checkCanceled()
                    if (file.isInLocalFileSystem && ProjectFileIndex.getInstance(project).isInContent(file) &&
                        !servers.any { server ->
                            server.descriptor.roots.any { root ->
                                VfsUtilCore.isAncestor(
                                    root,
                                    file,
                                    true
                                )
                            }
                        } &&
                        !descriptors.any { descriptor ->
                            descriptor.roots.any { root ->
                                VfsUtilCore.isAncestor(
                                    root,
                                    file,
                                    true
                                )
                            }
                        }
                    ) {
                        val starter = LspServerStarterImpl()
                        provider.fileOpened(project, file, starter)
                        descriptors.add(starter.descriptor)
                    }
                }
                descriptors
            }.expireWith(this).finishOnUiThread(ModalityState.nonModal()) { descriptors ->
                descriptors.forEach { descriptor ->
                    startNewServer(providerClass, descriptor)
                }


            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    fun getServersWithThisFileOpen(file: VirtualFile): Collection<LspServerImpl> {

        return ContainerUtil.findAll(this.servers) { it.isFileOpened(file) }
    }

    @RequiresEdt
    private fun startNewServer(
        providerClass: Class<out LspServerSupportProvider>,
        descriptor: LspServerDescriptor
    ) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (!servers.any { it.pluginClass == providerClass && it.descriptor.roots.contentEquals(descriptor.roots) }) {
            if (servers.size >= 10) {
                LOG.error("${servers.size} LSP servers already running and one more wants to start.\nTo save system resources, this request will be ignored: $descriptor")
            } else {
                WriteAction.run<Throwable> {
                    val server = LspServerImpl(providerClass, descriptor, listenersAdapter)
                    server.start()
                    servers.add(server)
                }
            }
        }
    }

    private fun stopServer(server: LspServer) {

        LOG.debug("$server: got stop server request")
        if (!this.servers.remove(server)) {
            LOG.error("LspServerManager doesn't know the server that it is asked to stop: $server")
        }

        (server as LspServerImpl).cleanupShutdownAndExit(false)
        this.highlightingQueue.queue(Update.create(this) {
            DaemonCodeAnalyzer.getInstance(this.project).restart()
        })
    }

    override fun stopServers(providerClass: Class<out LspServerSupportProvider>) {
        val servers = getServersForProvider(providerClass)
        for (server in servers) {
            stopServer(server)
        }
    }

    override fun stopAndRestartIfNeeded(providerClass: Class<out LspServerSupportProvider>) {
        stopServers(providerClass)
        startServersIfNeeded(providerClass)
    }


    @TestOnly
    @ApiStatus.Internal
    override fun addLspServerManagerListener(listener: LspServerManagerListener, parentDisposable: Disposable) {
        this.eventDispatcher.addListener(listener, parentDisposable)
        for (server in this.servers) {
            if (server.isMalfunctioned()) {
                this.eventDispatcher.multicaster.serverInitializationFailed()
            }

            val openedFiles = server.openedFiles
            for (file in openedFiles) {
                this.eventDispatcher.multicaster.fileOpened(file)
            }
        }
    }

    override fun dispose() {
        for (server in this.servers) {
            stopServer(server)
        }
    }

    fun onDiagnosticsReceived(virtualFile: VirtualFile) {
        ReadAction.run<Throwable> {
            if (project.isDisposed) {
                LOG.debug("Project disposed ", project)
            } else if (FileEditorManager.getInstance(project).isFileOpen(virtualFile)) {
                highlightingQueue.queue(Update.create(virtualFile) {
                    if (!virtualFile.isValid) {
                        LOG.debug("Virtual file was invalidated: ", virtualFile)
                    } else if (FileEditorManager.getInstance(project).isFileOpen(virtualFile)) {
                        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                        if (psiFile == null) {
                            LOG.debug("Unable to find a PsiFile for ", virtualFile)
                        } else {
                            DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                            listenersAdapter.diagnosticsReceived(virtualFile)
                        }
                    }
                })
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(LspServerManagerImpl::class.java)
        fun getInstanceImpl(project: Project): LspServerManagerImpl {
            return project.getService(LspServerManager::class.java) as LspServerManagerImpl
        }

        /**
         * 检查是否有lsp server正在运行
         */

        fun isLspRunning(): Boolean {
            val projectList = ProjectManager.getInstance().openProjects
//            var length = projectList.size

            for (project in projectList) {
//                val iterator = getInstanceImpl(project).servers.iterator()
//                while (iterator.hasNext()) {
//                    val next = iterator.next()
//                    if (next.isRunning()) {
//                        return true
//                    }
//                }
                for (server in getInstanceImpl(project).servers) {
                    if (server.isRunning()) {
                        return true
                    }
                }
            }
            return false
        }

        private fun getLspServerSupportProviders(): List<LspServerSupportProvider> {
            return LspServerSupportProvider.EP_NAME.extensionList
        }
    }


    class LspDocumentListener : DocumentListener {
        companion object {
            private val DOCUMENT_CHANGED_COALESCE_OBJECT = Any()
            private val documentsToHandle: MutableSet<Document> = HashSet()


            fun getFile(event: DocumentEvent): VirtualFile? {
                val file = FileDocumentManager.getInstance().getFile(event.document)
                return if (file != null && file.isInLocalFileSystem) {
                    file
                } else {
                    null
                }
            }
        }

        override fun documentChanged(event: DocumentEvent) {
            val file = getFile(event)
            if (file != null) {
                if (FileDocumentManager.getInstance().isDocumentUnsaved(event.document)) {
                    documentsToHandle.add(event.document)
                    handleDocuments()
                }
            }
        }

        private fun handleDocuments() {
            ReadAction.nonBlocking<ChangedFilesData> {
                val changedFilesData = ChangedFilesData()
                synchronized(documentsToHandle) {
                    changedFilesData.handledDocuments.addAll(documentsToHandle)
                }

                val fileDocumentManager = FileDocumentManager.getInstance()
                val projects = ProjectManager.getInstance().openProjects

                for (project in projects) {
                    ProgressManager.checkCanceled()
                    val servers = getInstanceImpl(project).servers

                    for (server in servers) {
                        ProgressManager.checkCanceled()
                        for (document in changedFilesData.handledDocuments) {
                            ProgressManager.checkCanceled()
                            if (fileDocumentManager.isDocumentUnsaved(document)) {
                                val file = fileDocumentManager.getFile(document)
                                if (file != null && file.isInLocalFileSystem) {
                                    if (server.isFileOpened(file)) {
                                        if (server.requiresFullSync()) {
                                            changedFilesData.serversToSendDidChange.add(
                                                Pair(
                                                    server,
                                                    DidChangeNotification.createFull(server, document, file)
                                                )
                                            )
                                        }
                                    } else if (server.hasCapabilities() && server.isSupportedFile(file)) {
                                        changedFilesData.serversToSendDidOpen.add(Pair(server, file))
                                    }
                                }
                            }
                        }
                    }
                }

                changedFilesData
            }.coalesceBy(DOCUMENT_CHANGED_COALESCE_OBJECT)
                .finishOnUiThread(ModalityState.nonModal()) { data ->
                    documentsToHandle.removeAll(data.handledDocuments)
                    if (data.serversToSendDidOpen.isNotEmpty() || data.serversToSendDidChange.isNotEmpty()) {
                        WriteAction.run<Throwable> {
                            data.serversToSendDidOpen.forEach { (server, file) ->
                                server.sendDidOpenRequest(file)
                            }
                            data.serversToSendDidChange.forEach { (server, notification) ->
                                server.requestExecutor.sendNotification(notification)
                            }
                        }
                    }
                }.submit(AppExecutorUtil.getAppExecutorService())
        }


        override fun beforeDocumentChange(event: DocumentEvent) {
            val file = getFile(event)
            if (file != null) {
                val projects = ProjectManager.getInstance().openProjects
                for (project in projects) {
                    val servers = getInstanceImpl(project).servers
                    for (server in servers) {
                        server.fileEdited(file, event)
                        if (server.requiresIncrementalSync() && server.isFileOpened(file)) {
                            server.requestExecutor.sendNotification(
                                DidChangeNotification.createIncrementalNotificationBeforeRealDocumentChange(
                                    server,
                                    event,
                                    file
                                )
                            )
                        }
                    }
                }
            }
        }


        private class ChangedFilesData {
            val handledDocuments: MutableSet<Document> = HashSet()
            val serversToSendDidChange = mutableListOf<Pair<LspServerImpl, DidChangeNotification>>()
            val serversToSendDidOpen = mutableListOf<Pair<LspServerImpl, VirtualFile>>()
        }
    }


    /**
     * 监听文件变化，并通知LSP server
     */
    class LspFileListener : AsyncFileListener {
        override fun prepareChange(events: MutableList<out VFileEvent>): ChangeApplier? {


            if (!isLspRunning()) {
                return null
            } else {
                val linkedHashSet = LinkedHashSet<VirtualFile>()
                val arrayList = ArrayList<VFileEvent>()
                for (file in events) {
                    if (file.fileSystem !is LocalFileSystem) continue
                    when (file) {
                        is VFileMoveEvent, is VFilePropertyChangeEvent -> {
                            if (file is VFilePropertyChangeEvent && file.isRename) {
                                linkedHashSet.add(file.file)
                            }
                        }

                        is VFileDeleteEvent, is VFileCreateEvent, is VFileCopyEvent, is VFileContentChangeEvent -> {
                            arrayList.add(file)
                        }
                    }
                }
                return if (linkedHashSet.isEmpty() && arrayList.isEmpty()) null else FileChangeApplier(
                    linkedHashSet,
                    arrayList
                )
            }
        }


        class FileChangeApplier(
            private val renamedFilesAndDirs: Set<VirtualFile>,
            private val deleteCreateCopyChangeEvents: List<VFileEvent>
        ) : ChangeApplier {


            private val serversToUpdateOpenedFiles: MutableSet<LspServerImpl>
            private val fileChangeInfos: MultiMap<LspServerImpl, LspServerImpl.FileChangeInfo>

            init {
                this.serversToUpdateOpenedFiles = HashSet()
                this.fileChangeInfos = MultiMap.create()
            }


            override fun beforeVfsChange() {
                val projects = ProjectManager.getInstance().openProjects

                for (project in projects) {
                    val servers = getInstanceImpl(project).servers

                    for (server in servers) {
                        val hasCapability =
                            server.dynamicCapabilities.hasCapability(LspDynamicCapabilities.didChangeWatchedFiles)

                        if (hasCapability) {
                            for (event in deleteCreateCopyChangeEvents) {
                                if (event is VFileDeleteEvent) {
                                    val file = event.file
                                    val fileUri = server.descriptor.getFileUri(file)
                                    fileChangeInfos.putValue(
                                        server,
                                        LspServerImpl.FileChangeInfo(fileUri, file.isDirectory, FileChangeType.Deleted)
                                    )
                                }
                            }
                        }

                        for (renamedFile in renamedFilesAndDirs) {
                            if (hasCapability) {
                                val fileUri = server.descriptor.getFileUri(renamedFile)
                                fileChangeInfos.putValue(
                                    server,
                                    LspServerImpl.FileChangeInfo(
                                        fileUri,
                                        renamedFile.isDirectory,
                                        FileChangeType.Deleted
                                    )
                                )
                            }

                            val openedFiles = server.openedFiles

                            for (openedFile in openedFiles) {
                                if (VfsUtilCore.isAncestor(renamedFile, openedFile, false)) {

                                    serversToUpdateOpenedFiles.add(server)
                                    server.sendDidCloseRequest(openedFile)
                                }
                            }
                        }
                    }
                }
            }

            override fun afterVfsChange() {
                val projects = ProjectManager.getInstance().openProjects

                for (project in projects) {
                    val servers = getInstanceImpl(project).servers

                    for (server in servers) {
                        val hasCapability =
                            server.dynamicCapabilities.hasCapability(LspDynamicCapabilities.didChangeWatchedFiles)

                        if (hasCapability) {
                            for (renamedFile in renamedFilesAndDirs) {
                                val fileUri = server.descriptor.getFileUri(renamedFile)
                                fileChangeInfos.putValue(
                                    server,
                                    LspServerImpl.FileChangeInfo(
                                        fileUri,
                                        renamedFile.isDirectory,
                                        FileChangeType.Created
                                    )
                                )
                            }

                            for (event in deleteCreateCopyChangeEvents) {
                                val fileUri: String
                                val file: VirtualFile
                                val changeType: FileChangeType

                                when (event) {
                                    is VFileContentChangeEvent -> {
                                        file = event.file
                                        fileUri = server.descriptor.getFileUri(file)
                                        changeType = FileChangeType.Changed
                                    }

                                    is VFileCreateEvent -> {
                                        file = event.file ?: continue
                                        fileUri = server.descriptor.getFileUri(file)
                                        changeType = FileChangeType.Created
                                    }

                                    is VFileCopyEvent -> {
                                        file = event.findCreatedFile() ?: continue
                                        fileUri = server.descriptor.getFileUri(file)
                                        changeType = FileChangeType.Created
                                    }

                                    else -> continue
                                }

                                fileChangeInfos.putValue(
                                    server,
                                    LspServerImpl.FileChangeInfo(fileUri, file.isDirectory, changeType)
                                )
                            }
                        }
                    }
                }

                if (!fileChangeInfos.isEmpty) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        for ((server, fileChangeInfos) in fileChangeInfos.entrySet()) {
                            server.processFileEvents(fileChangeInfos)
                        }
                    }
                }

                for (server in serversToUpdateOpenedFiles) {
                    server.sendOpenedFiles()
                }
            }
        }


    }


    class LspFileEditorManagerListener : FileEditorManagerListener {

        private val OPEN_FILES_COALESCE_OBJECT = Any()
        private val openedFilesToHandle: MutableSet<VirtualFile> = HashSet()

        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            if (file.isInLocalFileSystem) {
                val serverManager = getInstanceImpl(source.project)
                if (serverManager.servers.isNotEmpty() || getLspServerSupportProviders().isNotEmpty()) {
                    openedFilesToHandle.add(file)
                    handleFiles(source.project)
                }
            }
        }

        private fun handleFiles(project: Project) {

            val lspServerManager = getInstanceImpl(project)
            ReadAction.nonBlocking<OpenedFilesData> {
                val openedFilesData = OpenedFilesData()
                synchronized(openedFilesToHandle) {
                    openedFilesData.handledFiles.addAll(openedFilesToHandle)
                }

                for (lspServerSupportProvider in getLspServerSupportProviders()) {
                    ProgressManager.checkCanceled()
                    val servers = lspServerManager.getServersForProvider(lspServerSupportProvider::class.java)
                    var isAncestor = false
                    for (file in openedFilesData.handledFiles) {
                        ProgressManager.checkCanceled()
                        for (server in servers) {
                            ProgressManager.checkCanceled()
                            if (server.descriptor.roots.any { root -> VfsUtilCore.isAncestor(root, file, true) }) {
                                isAncestor = true
                            }

                            val lspServer = server as LspServerImpl
                            if (lspServer.hasCapabilities() && !lspServer.isFileOpened(file) && lspServer.isSupportedFile(
                                    file
                                )
                            ) {
                                openedFilesData.serversToSendDidOpen.putValue(lspServer, file)
                            }
                        }

                        if (!isAncestor && ProjectFileIndex.getInstance(project).isInContent(file)) {
                            val lspServerStarter = LspServerStarterImpl()
                            lspServerSupportProvider.fileOpened(project, file, lspServerStarter)
                            lspServerStarter.descriptor?.let {
                                openedFilesData.newServersToStart.add(Pair(lspServerSupportProvider, it))
                            }
                        }
                    }
                }

                openedFilesData
            }.coalesceBy(project, OPEN_FILES_COALESCE_OBJECT).expireWith(lspServerManager)
                .finishOnUiThread(ModalityState.nonModal()) { data ->
                    openedFilesToHandle.removeAll(data.handledFiles)
                    if (!data.serversToSendDidOpen.isEmpty) {
                        WriteAction.run<Throwable> {
                            for ((server, files) in data.serversToSendDidOpen.entrySet()) {
                                val lspServer = server as LspServerImpl
                                for (file in files) {
                                    lspServer.sendDidOpenRequest(file)
                                }
                            }
                        }
                    }

                    data.newServersToStart.forEach { (provider, descriptor) ->
                        lspServerManager.startNewServer(provider::class.java, descriptor)
                    }
                }.submit(AppExecutorUtil.getAppExecutorService())
        }


        override fun fileClosed(source: FileEditorManager, file: VirtualFile) {

            if (file.isInLocalFileSystem && !source.isFileOpen(file)) {
                val document = FileDocumentManager.getInstance().getCachedDocument(file)
                if (document != null && !FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                    val servers = getInstanceImpl(source.project).servers
                        .filter { it.isFileOpened(file) }
                    if (servers.isNotEmpty()) {
                        WriteAction.run<Throwable> {
                            servers.forEach { server ->
                                server.sendDidCloseRequest(file)
                            }
                        }
                    }
                }
            }
        }


        private class OpenedFilesData {
            val handledFiles: MutableSet<VirtualFile> = HashSet()
            val serversToSendDidOpen: MultiMap<LspServerImpl, VirtualFile> = MultiMap.create()
            val newServersToStart: MutableCollection<Pair<LspServerSupportProvider, LspServerDescriptor>> =
                SmartList()
        }

    }

    class LspFileDocumentManagerListener : FileDocumentManagerListener {
        companion object {
            private val CLOSE_FILES_COALESCE_OBJECT = Any()
        }

        override fun beforeDocumentSaving(document: Document) {
            val file = FileDocumentManager.getInstance().getFile(document)
            if (file != null && file.isInLocalFileSystem) {
                val projects = ProjectManager.getInstance().openProjects
                for (project in projects) {
                    val serverManager = getInstanceImpl(project)
                    if (serverManager.servers.isNotEmpty() && !FileEditorManager.getInstance(project)
                            .isFileOpen(file)
                    ) {
                        checkServer(serverManager)
                    }
                }
            }
        }

        private fun checkServer(serverManager: LspServerManagerImpl) {
            ReadAction.nonBlocking<MultiMap<LspServerImpl, VirtualFile>> {
                val filesToClose = MultiMap.create<LspServerImpl, VirtualFile>()
                for (server in serverManager.servers) {
                    val files = server.getFilesToClose()
                    if (files.isNotEmpty()) {
                        filesToClose.put(server, files)
                    }
                }
                filesToClose
            }.expireWith(serverManager)
                .coalesceBy(serverManager, CLOSE_FILES_COALESCE_OBJECT)
                .finishOnUiThread(ModalityState.nonModal()) { map ->
                    if (!map.isEmpty) {
                        WriteAction.run<Throwable> {
                            for ((server, files) in map.entrySet()) {
                                for (file in files) {
                                    server.sendDidCloseRequest(file)
                                }
                            }
                        }
                    }
                }.submit(AppExecutorUtil.getAppExecutorService())
        }

        override fun unsavedDocumentDropped(document: Document) {

            beforeDocumentSaving(document)
        }
    }


    private class LspServerStarterImpl : LspServerSupportProvider.LspServerStarter {
        var descriptor: LspServerDescriptor? = null
        override fun ensureServerStarted(descriptor: LspServerDescriptor) {

            this.descriptor = descriptor
        }
    }


}

