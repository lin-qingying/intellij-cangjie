package com.linqingying.lsp.impl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.containers.MultiMap
import com.linqingying.lsp.api.LspServerState
import org.eclipse.lsp4j.FileChangeType

internal class LspFileListener : AsyncFileListener {
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? { 
        if (!LspServerManagerImpl.isAnyServerRunning()) {
            return null
        }

        val movedFiles = mutableSetOf<VirtualFile>()
        val changeEvents = mutableListOf<VFileEvent>()

        for (event in events) {
            if (event.fileSystem !is LocalFileSystem) continue

            when (event) {
                is VFileMoveEvent -> {
                    movedFiles.add(event.file )
                }

                is VFilePropertyChangeEvent -> {
                    if (event.isRename) {
                        movedFiles.add(event.file )
                    }
                }

                is VFileDeleteEvent, is VFileCreateEvent, is VFileCopyEvent, is VFileContentChangeEvent -> {
                    changeEvents.add(event)
                }
            }
        }

        return if (movedFiles.isEmpty() && changeEvents.isEmpty()) {
            null
        } else {
            FileChangeApplier(movedFiles, changeEvents)
        }
    }

    private class FileChangeApplier(
        val renamedFilesAndDirs: Set<VirtualFile>, val deleteCreateCopyChangeEvents: List<VFileEvent>
    ) : AsyncFileListener.ChangeApplier {


        private val serverToFileChangeInfos: MultiMap<LspServerImpl, LspServerImpl.FileChangeInfo> =
            MultiMap.create()

        private val serversToUpdateOpenedFiles: MutableSet<LspServerImpl> = mutableSetOf()

        override fun afterVfsChange() {
            val projectManager = ProjectManager.getInstance()
            val openProjects = projectManager.openProjects

            for (project in openProjects) {
                val lspServerManager = LspServerManagerImpl.Companion
                val lspServers = lspServerManager.getInstanceImpl(project).lspServers

                for (server in lspServers) {
                    if (server.state == LspServerState.Running) {
                        if (server.dynamicCapabilities.hasCapability(LspDynamicCapabilities.didChangeWatchedFiles)) {
                            for (file in renamedFilesAndDirs) {
                                val fileUri = server.descriptor.getFileUri(file)
                                serverToFileChangeInfos.putValue(
                                    server,
                                    LspServerImpl.FileChangeInfo(
                                        file.path,
                                        fileUri,
                                        file.isDirectory,
                                        FileChangeType.Created
                                    )
                                )
                            }

                            for (event in deleteCreateCopyChangeEvents) {
                                when (event) {
                                    is VFileContentChangeEvent -> {
                                        val file = event.file
                                        val fileUri = server.descriptor.getFileUri(file)
                                        serverToFileChangeInfos.putValue(
                                            server,
                                            LspServerImpl.FileChangeInfo(
                                                file.path,
                                                fileUri,
                                                file.isDirectory,
                                                FileChangeType.Changed
                                            )
                                        )
                                    }

                                    is VFileCreateEvent -> {
                                        val file = event.file
                                        if (file != null) {
                                            val fileUri = server.descriptor.getFileUri(file)
                                            serverToFileChangeInfos.putValue(
                                                server,
                                                LspServerImpl.FileChangeInfo(
                                                    file.path,
                                                    fileUri,
                                                    file.isDirectory,
                                                    FileChangeType.Created
                                                )
                                            )
                                        }
                                    }

                                    is VFileCopyEvent -> {
                                        val createdFile = event.findCreatedFile()
                                        if (createdFile != null) {
                                            val fileUri = server.descriptor.getFileUri(createdFile)
                                            serverToFileChangeInfos.putValue(
                                                server,
                                                LspServerImpl.FileChangeInfo(
                                                    createdFile.path,
                                                    fileUri,
                                                    createdFile.isDirectory,
                                                    FileChangeType.Created
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!serverToFileChangeInfos.isEmpty) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    for ((server, fileChangeInfos) in  serverToFileChangeInfos.entrySet()) {

                        server.processFileEvents(fileChangeInfos)
                    }
                }
            }

            for (server in serversToUpdateOpenedFiles) {
                server.sendOpenedFiles()
            }
        }

        override fun beforeVfsChange() {
            val openProjects = ProjectManager.getInstance().openProjects

            for (project in openProjects) {

                val servers = LspServerManagerImpl.getInstanceImpl(project).lspServers

                for (server in servers) {
                    if (server.state == LspServerState.Running) {
                        val hasCapability =
                            server.dynamicCapabilities.hasCapability(LspDynamicCapabilities.didChangeWatchedFiles)

                        if (hasCapability) {
                            for (event in deleteCreateCopyChangeEvents) {
                                if (event is VFileDeleteEvent) {
                                    val file = event.file

                                    val uri = server.descriptor.getFileUri(file)
                                    serverToFileChangeInfos.putValue(
                                        server,
                                        LspServerImpl.FileChangeInfo(
                                            file.path,
                                            uri,
                                            file.isDirectory,
                                            FileChangeType.Deleted
                                        )
                                    )
                                }
                            }
                        }

                        for (renamedFile in renamedFilesAndDirs) {
                            if (hasCapability) {
                                val uri = server.descriptor.getFileUri(renamedFile)
                                serverToFileChangeInfos.putValue(
                                    server,
                                    LspServerImpl.FileChangeInfo(
                                        renamedFile.path,
                                        uri,
                                        renamedFile.isDirectory,
                                        FileChangeType.Deleted
                                    )
                                )
                            }

                            val filesToClose = mutableListOf<VirtualFile>()
                            for (openedFile in server.openedFiles) {
                                if (VfsUtilCore.isAncestor(renamedFile, openedFile, false)) {
                                    serversToUpdateOpenedFiles.add(server)
                                    filesToClose.add(openedFile)
                                }
                            }

                            for (fileToClose in filesToClose) {
                                server.sendDidCloseRequest(fileToClose)
                            }
                        }
                    }
                }
            }
        }
    }
}
