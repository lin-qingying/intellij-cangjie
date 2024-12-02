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


import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notebook.editor.BackedVirtualFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MAX_LSP_SERVERS: Int = 10

val logger: Logger = Logger.getInstance(
    LspServerManagerImpl::class.java
)

class LspServerManagerImpl internal constructor(
    val project: Project,
    val cs: CoroutineScope
) : LspServerManager, Disposable {
    companion object {
        internal inline fun forEachRunningServerInEachProject(action: (LspServerImpl) -> Unit) {
            val projects = ProjectManager.getInstance().openProjects

            for (project in projects) {
                val lspServers = getInstanceImpl(project).lspServers
                for (server in lspServers) {
                    if (server.state == LspServerState.Running) {
                        action(server)
                    }
                }
            }
        }

        internal suspend fun restartCodeHighlighting(
            project: Project,
            file: VirtualFile
        ) = // 执行 UI 线程相关的操作
            // 在这里我们调用 readAction 来确保操作在 UI 线程上执行
            readAction {
                val originFile = BackedVirtualFile.getOriginFileIfBacked(file)

                // 检查文件是否已经打开
                if (FileEditorManager.getInstance(project).isFileOpen(originFile)) {
                    val psiFile = PsiManager.getInstance(project).findFile(file)
                    psiFile?.let {
                        DaemonCodeAnalyzer.getInstance(project).restart(it)  // 重启代码高亮
                    }
                }
            }

        fun getInstanceImpl(project: Project): LspServerManagerImpl {
            return LspServerManager.getInstance(project) as LspServerManagerImpl
        }

        internal fun isAnyServerRunning(): Boolean {

            val projects = ProjectManager.getInstance().openProjects

            for (project in projects) {
                val lspServers = getInstanceImpl(project).lspServers

                if (lspServers.any { it.state == LspServerState.Running }) {
                    return true
                }
            }
            return false
        }
    }

    private val CLOSE_FILES_COALESCE_OBJECT: Any = Any()

    private val START_SERVER_COALESCE_OBJECT: Any = Any()
    private val eventBroadcaster: LspServerManagerListener
    private val eventDispatcher: EventDispatcher<LspServerManagerListener> = EventDispatcher.create(
        LspServerManagerListener::class.java
    )

    init {
        if (project.isDefault) {
            throw AssertionError("LspServerManager doesn't make sense for the default project")

        } else {
            registerExtensionListener()
            startCoroutine()

            this.eventBroadcaster = object : LspServerManagerListener {
                override fun serverStateChanged(lspServer: LspServer) {
                    eventDispatcher.multicaster.serverStateChanged(lspServer)
                }

                override fun diagnosticsReceived(lspServer: LspServer, file: VirtualFile) {
                    eventDispatcher.multicaster.diagnosticsReceived(lspServer, file)
                }

                override fun fileOpened(lspServer: LspServer, file: VirtualFile) {
                    eventDispatcher.multicaster.fileOpened(lspServer, file)
                }
            }

        }

    }


    private val highlightingQueue: MergingUpdateQueue = MergingUpdateQueue(
        "LSP highlighting queue", 100, true, null,
        this, null, true
    )

    val lspServers: MutableCollection<LspServerImpl> = ContainerUtil.createLockFreeCopyOnWriteList()


    @org.jetbrains.annotations.ApiStatus.Internal
    override fun addLspServerManagerListener(
        listener: LspServerManagerListener,
        parentDisposable: Disposable,
        sendEventsForExistingServers: Boolean
    ) {
        eventDispatcher.addListener(listener, parentDisposable)

        if (sendEventsForExistingServers) {
            for (server in lspServers) {
                if (server.state == LspServerState.ShutdownUnexpectedly) {
                    eventDispatcher.multicaster.serverStateChanged(server)
                }

                for (file in server.openedFiles) {
                    eventDispatcher.multicaster.fileOpened(server, file)
                }
            }
        }
    }


    private fun startCoroutine() {


        this.cs.launch {
            try {
                val eventLog = WorkspaceModel.getInstance(project).eventLog

                eventLog.collect { value ->
                    if (value.getChanges(ContentRootEntity::class.java).isNotEmpty()) {
                        onProjectRootsChanged()
                    }
                }
            } catch (_: NoSuchMethodError) {

            }


        }

    }

    override fun dispose() {

        for (server in this.lspServers) {
            stopRunningServer(server)
        }
    }

    @RequiresEdt
    override fun ensureServerStarted(
        providerClass: Class<out LspServerSupportProvider>,
        descriptor: LspServerDescriptor
    ) {
        cs.launch {
            readAction {
                val lspServer = LspServerImpl(providerClass, descriptor, eventBroadcaster)
                lspServer.start()
                lspServers.add(lspServer)


            }
        }
//
//        if (lspServers.any {
//                it.providerClass == providerClass && it.descriptor.roots.contentEquals(descriptor.roots)
//            }) {
//            return
//        }
//
//        if (lspServers.size >= 10) {
//
//            logger.error("${lspServers.size} LSP servers are already running and one more wants to start. To save system resources, this request will be ignored: $descriptor")
//        } else {
//            WriteAction.run<Throwable> { startCoroutine() }
//        }
    }

    internal inline fun findRunningServer(condition: (LspServerImpl) -> Boolean): LspServerImpl? {
        for (server in lspServers) {
            if (server.state == LspServerState.Running && condition(server)) {
                return server
            }
        }
        return null
    }

    override fun getServersForProvider(providerClass: Class<out LspServerSupportProvider>): Collection<LspServerImpl> {
        return lspServers.toList()
            .filter { it.providerClass == providerClass }
    }

    internal fun getServersWithThisFileOpen(file: VirtualFile): Collection<LspServerImpl> {
        return lspServers.toList()
            .filter { it.isFileOpened(file) }
    }

    internal fun handleMaybeUnexpectedServerStop(
        lspServer: LspServerImpl,
        serverOutput: String
    ) {
        lspServer.ensureServerStopped(false) {

            if (lspServer.state != LspServerState.ShutdownNormally) {
                lspServer.appendServerErrorOutput(serverOutput)
            }
            this@LspServerManagerImpl.stopServer(lspServer, false)

        }
    }

    private fun stopServer(server: LspServerImpl, shouldRemove: Boolean) {
        val validStates = arrayOf(LspServerState.Initializing, LspServerState.Running)

        if (server.state in validStates && !lspServers.contains(server)) {
            logger.error("LspServerManager doesn't know the server that it is asked to stop: $server")
        }

        if (shouldRemove) {
            lspServers.remove(server)
        }

        if (server.state === LspServerState.Running) {
            DaemonCodeAnalyzer.getInstance(this.project).restart()
        }
//        if (listOf(LspServerState.ShutdownNormally, LspServerState.ShutdownUnexpectedly).contains(server.state)) {
//            if (server.state == LspServerState.Running) {
//                highlightingQueue.queue(Update.create(this) {
//
//                    DaemonCodeAnalyzer.getInstance(project).restart()
//
//                })
//            }
//            server.cleanupShutdownAndExit(shouldRemove)
//
//        }
//        if (server.state == LspServerState.Running) {
//            highlightingQueue.queue(Update.create(this) {
//                cs.launch {
//                    val flow = getInstance(project).eventLog
//                    flow.collect { event ->
//                        if (event.getChanges(ContentRootEntity::class.java).isNotEmpty()) {
//                            onProjectRootsChanged()
//                        }
//                    }
//                }
//            })
//        }
    }

    internal fun onDiagnosticsReceived(
        lspServer: LspServer,
        virtualFile: VirtualFile
    ) {

        cs.launch {
            restartCodeHighlighting(project, virtualFile)

            eventBroadcaster.diagnosticsReceived(lspServer, virtualFile)

        }

    }


    suspend fun onProjectRootsChanged() {
        readAction {
            val openFiles = FileEditorManager.getInstance(this@LspServerManagerImpl.project).openFiles
            val documents = FileDocumentManager.getInstance().unsavedDocuments
            val virtualFiles = mutableListOf<VirtualFile>()
            for (document in documents) {
                val file = FileDocumentManager.getInstance().getFile(document)
                file?.let { virtualFiles.add(it) }
            }

            val allFiles = virtualFiles + openFiles.toList()
            LspOpenedFilesService.getInstance(project).processOpenedFiles(allFiles)
        }
    }

    internal fun scheduleClosingFilesThatAreNotOfInterest() {
        if (lspServers.isNotEmpty()) {
            nonBlocking<MultiMap<LspServerImpl, VirtualFile>> { closeFiles() }
                .expireWith(this)
                .coalesceBy(CLOSE_FILES_COALESCE_OBJECT)
                .finishOnUiThread(ModalityState.nonModal()) {

                    if (!it.isEmpty) {
                        runWriteAction {
                            for ((server, filesToClose) in it.entrySet()) {

                                for (virtualFile in filesToClose) {

                                    server.sendDidCloseRequest(virtualFile)
                                }
                            }
                        }

                    }

                }
                .submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    private fun closeFiles(): MultiMap<LspServerImpl, VirtualFile> {
        val multiMap = MultiMap<LspServerImpl, VirtualFile>()
        for (lspServer in lspServers) {
            val filesToClose = lspServer.getFilesToClose()
            if (filesToClose.isNotEmpty()) {
                multiMap.put(lspServer, filesToClose)
            }
        }
        return multiMap
    }

    override fun startServersIfNeeded(providerClass: Class<out LspServerSupportProvider>): Unit {
        val serverSupportProvider = LspServerSupportProvider.EP_NAME.findExtension(providerClass)

        if (serverSupportProvider == null) {
            logger.error("${providerClass.name} is not loaded")
        } else {
            cs.launch {
                val list = readAction {
                    val servers = getServersForProvider(providerClass)
                    val result = SmartList<LspServerDescriptor>()
                    val openFiles = FileEditorManager.getInstance(project).openFiles

                    openFiles.forEach { file ->
                        ProgressManager.checkCanceled()
                        if (file.isInLocalFileSystem && ProjectFileIndex.getInstance(project).isInContent(file)) {
                            val isAssociatedWithServer = servers.any { server ->
                                server.descriptor.roots.any { VfsUtilCore.isAncestor(it, file, true) }
                            }

                            if (!isAssociatedWithServer) {
                                val isAssociatedWithDescriptor = result.any { descriptor ->
                                    descriptor.roots.any { VfsUtilCore.isAncestor(it, file, true) }
                                }

                                if (!isAssociatedWithDescriptor) {
                                    val starter = LspServerStarterImpl()
                                    serverSupportProvider.fileOpened(project, file, starter)
                                    starter.descriptor?.let { result.add(it) }
                                }
                            }
                        }
                    }

                    result
                }

                list.forEach {
                    ensureServerStarted(providerClass, it)
                }
            }

//            nonBlocking<SmartList<LspServerDescriptor>> {
//                val servers = getServersForProvider(providerClass)
//                val result = SmartList<LspServerDescriptor>()
//                val openFiles = FileEditorManager.getInstance(project).openFiles
//
//                openFiles.forEach { file ->
//                    ProgressManager.checkCanceled()
//                    if (file.isInLocalFileSystem && ProjectFileIndex.getInstance(project).isInContent(file)) {
//                        val isAssociatedWithServer = servers.any { server ->
//                            server.descriptor.roots.any { VfsUtilCore.isAncestor(it, file, true) }
//                        }
//
//                        if (!isAssociatedWithServer) {
//                            val isAssociatedWithDescriptor = result.any { descriptor ->
//                                descriptor.roots.any { VfsUtilCore.isAncestor(it, file, true) }
//                            }
//
//                            if (!isAssociatedWithDescriptor) {
//                                val starter = LspServerStarterImpl()
//                                serverSupportProvider.fileOpened(project, file, starter)
//                                starter.descriptor?.let { result.add(it) }
//                            }
//                        }
//                    }
//                }
//
//                result
//            }
//                .expireWith(this)
//                .coalesceBy(providerClass, START_SERVER_COALESCE_OBJECT)
//                .finishOnUiThread(ModalityState.nonModal()) { descriptors ->
//                    descriptors.forEach { descriptor ->
//                        startNewServer(providerClass, descriptor)
//                    }
//
//                }
//                .submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    private fun onDiagnosticsReceived(virtualFile: VirtualFile, lspServer: LspServer) {

        val fileEditorManager = FileEditorManager.getInstance(project)

        if (fileEditorManager.isFileOpen(virtualFile)) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            psiFile?.let {
                DaemonCodeAnalyzer.getInstance(project).restart(it)
                eventBroadcaster.diagnosticsReceived(lspServer, virtualFile)
            }
        }
    }

    private fun registerExtensionListener() {
        LspServerSupportProvider.EP_NAME.point.addExtensionPointListener(object :
            ExtensionPointListener<LspServerSupportProvider> {
            override fun extensionAdded(extension: LspServerSupportProvider, pluginDescriptor: PluginDescriptor) {

                startServersIfNeeded(extension::class.java)
            }


            override fun extensionRemoved(extension: LspServerSupportProvider, pluginDescriptor: PluginDescriptor) {

                stopServers(extension::class.java)
            }
        }, false, this)

    }

    @RequiresEdt
    private fun startNewServer(
        providerClass: Class<out LspServerSupportProvider>,
        descriptor: LspServerDescriptor
    ) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (!lspServers.any { it.providerClass == providerClass && it.descriptor.roots.contentEquals(descriptor.roots) }) {
            if (lspServers.size >= 10) {
                logger.error("${lspServers.size} LSP servers already running and one more wants to start.\nTo save system resources, this request will be ignored: $descriptor")
            } else {
                runWriteAction {
                    val server = LspServerImpl(providerClass, descriptor, eventBroadcaster)
                    server.start()
                    lspServers.add(server)
                }
            }
        }
    }

    override fun stopAndRestartIfNeeded(providerClass: Class<out LspServerSupportProvider>) {
        this.stopServers(providerClass)
        this.startServersIfNeeded(providerClass)
    }

    internal fun stopRunningServer(lspServer: LspServerImpl) {
        lspServer.ensureServerStopped(true) {
            handleServerStop(lspServer, true)
        }
    }

    private fun handleServerStop(lspServer: LspServerImpl, removeFromList: Boolean) {
        val activeStates = arrayOf(LspServerState.Initializing, LspServerState.Running)

        if (activeStates.contains(lspServer.state) && !lspServers.contains(lspServer)) {
            logger.error("LspServerManager doesn't know the server that it is asked to stop: $lspServer")
        }

        if (removeFromList) {
            lspServers.remove(lspServer)
        }

        if (lspServer.state == LspServerState.Running) {
            highlightingQueue.queue(Update.create(this) {
                startCoroutine()
            })
        }
    }

    override fun stopServers(providerClass: Class<out LspServerSupportProvider>) {
        for (server in getServersForProvider(providerClass)) {
            stopRunningServer(server)
        }
    }

    internal class LspServerStarterImpl :
        LspServerSupportProvider.LspServerStarter {
        var descriptor: LspServerDescriptor? = null

        override fun ensureServerStarted(descriptor: LspServerDescriptor) {
            this.descriptor = descriptor

        }
    }
}
