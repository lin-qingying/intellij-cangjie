package com.linqingying.lsp.impl

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.MultiMap
import com.linqingying.lsp.api.LspServerDescriptor
import com.linqingying.lsp.api.LspServerState
import com.linqingying.lsp.api.LspServerSupportProvider
import java.util.*

@Service(Service.Level.PROJECT)
internal class LspOpenedFilesService(val project: Project) {
    companion object {
        fun getInstance(project: Project): LspOpenedFilesService {
            return project.service<LspOpenedFilesService>()
        }
    }

    private val openedFilesToHandle: MutableSet<VirtualFile> = Collections.synchronizedSet(HashSet())


    fun processOpenedFiles(files: Collection<VirtualFile>) {
        if (LspServerSupportProvider.EP_NAME.hasAnyExtensions()) {
            val localFiles = files.filter { it.isInLocalFileSystem }

            if (openedFilesToHandle.addAll(localFiles)) {
                startProcessing()
            }
        }
    }

    class OpenedFilesData {
        val handledFiles: MutableSet<VirtualFile> = mutableSetOf()
        val serversToSendDidOpen: MultiMap<LspServerImpl, VirtualFile> = MultiMap()
        val newServersToStart: MutableList<Pair<LspServerSupportProvider, LspServerDescriptor>> = mutableListOf()
    }

    private fun startProcessing() {
        val instance = LspServerManagerImpl.getInstanceImpl(project)
        nonBlocking<OpenedFilesData> {

            val filesData = OpenedFilesData()
            synchronized(openedFilesToHandle) {
                filesData.handledFiles.addAll(openedFilesToHandle)
            }
            LspServerSupportProvider.EP_NAME.extensionList.forEach { provider ->
                val serversForProvider: Collection<LspServerImpl> =
                    instance.getServersForProvider(provider::class.java)
                filesData.handledFiles.forEach { virtualFile ->


                    var isAncestorFound = false
                    serversForProvider.forEach { lspServer ->
                        ProgressManager.checkCanceled()
                        val roots = lspServer.descriptor.roots

                        if (roots.any { VfsUtilCore.isAncestor(it, virtualFile, true) }) {
                            isAncestorFound = true
                        }

                        if (lspServer.state == LspServerState.Running &&
                            !lspServer.isFileOpened(virtualFile) &&
                            lspServer.isSupportedFile(virtualFile)
                        ) {
                            filesData.serversToSendDidOpen.putValue(lspServer, virtualFile)
                        }

                    }

                    if (!isAncestorFound && ProjectFileIndex.getInstance(project).isInContent(virtualFile)) {
                        val lspServerStarter = LspServerManagerImpl.LspServerStarterImpl()
                        provider.fileOpened(project, virtualFile, lspServerStarter)
                        val descriptor = lspServerStarter.descriptor

                        descriptor?.let {
                            filesData.newServersToStart.add(provider to it)
                        }
                    }
                }

            }

            filesData
        }.coalesceBy(this).finishOnUiThread(ModalityState.nonModal()) {
            val openedFilesToHandle = openedFilesToHandle
            openedFilesToHandle.removeAll(it.handledFiles)

            if (!it.serversToSendDidOpen.isEmpty) {
                runWriteAction {
                    it.serversToSendDidOpen.entrySet().forEach { (lspServerImpl, virtualFiles) ->
                        virtualFiles.forEach { virtualFile ->
                            lspServerImpl.sendDidOpenRequest(virtualFile)
                        }
                    }
                }
            }

            val newServersToStart = it.newServersToStart


            newServersToStart.forEach { (provider, descriptor) ->
                instance.ensureServerStarted(provider::class.java, descriptor)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }
}

