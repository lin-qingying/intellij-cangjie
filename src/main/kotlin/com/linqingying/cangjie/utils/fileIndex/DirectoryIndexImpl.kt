// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.linqingying.cangjie.utils.fileIndex

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiDirectoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.Query
import com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexEx
import  com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetWithCustomData

import kotlin.concurrent.Volatile

//
//class DirectoryIndexImpl(private val myProject: Project) : DirectoryIndex(),
//    Disposable {
//    private val myConnection = myProject.messageBus.connect()
//    private val myWorkspaceFileIndex = WorkspaceFileIndex.getInstance(myProject) as WorkspaceFileIndexEx
//
//    @Volatile
//    private var myDisposed = false
//
//    @Volatile
//    private var myRootIndex: RootIndex? = null
//
//    init {
//        subscribeToFileChanges()
//    }
//
//    override fun dispose() {
//        myDisposed = true
//        myRootIndex = null
//    }
//
//    private fun subscribeToFileChanges() {
//        myConnection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
//            override fun after(events: List<VFileEvent>) {
//                val rootIndex = myRootIndex
//                if (rootIndex != null && shouldResetOnEvents(events)) {
//                    for (event in events) {
//                        if (isIgnoredFileCreated(event)) {
//                            reset()
//                            break
//                        }
//                    }
//                }
//            }
//        })
//    }
//
//    public override fun getDirectoriesByPackageName(
//        packageName: String,
//        includeLibrarySources: Boolean
//    ): Query<VirtualFile> {
//        return myWorkspaceFileIndex.getDirectoriesByPackageName(packageName, includeLibrarySources)
//    }
//
//    public override fun getDirectoriesByPackageName(
//        packageName: String,
//        scope: GlobalSearchScope
//    ): Query<VirtualFile> {
//        return myWorkspaceFileIndex.getDirectoriesByPackageName(packageName, scope)
//    }
//
//    private val rootIndex: RootIndex
//        get() {
//            var rootIndex = myRootIndex
//            if (rootIndex == null) {
//                rootIndex = RootIndex(myProject)
//                myRootIndex = rootIndex
//            }
//            return rootIndex
//        }
//
//    public override fun getPackageName(dir: VirtualFile): String? {
//        checkAvailability()
//        return myWorkspaceFileIndex.getPackageName(dir)
//    }
//
//    public override fun getOrderEntries(fileOrDir: VirtualFile): List<OrderEntry> {
//        checkAvailability()
//        if (myProject.isDefault) return emptyList()
//        val fileInfo = myWorkspaceFileIndex.getFileInfo(fileOrDir, true, true, true, true, false)
//        val fileSet = fileInfo.findFileSet { data: WorkspaceFileSetWithCustomData<*>? -> true }
//        if (fileSet == null) return emptyList()
//        return rootIndex.getOrderEntries(fileSet.root)
//    }
//
//    public override fun getDependentUnloadedModules(module: Module): Set<String> {
//        checkAvailability()
//        return rootIndex.getDependentUnloadedModules(module)
//    }
//
//    private fun checkAvailability() {
//        ApplicationManager.getApplication().assertReadAccessAllowed()
//        if (myDisposed) {
//            ProgressManager.checkCanceled()
//            LOG.error("Directory index is already disposed for $myProject")
//        }
//    }
//
//    fun reset() {
//        myRootIndex = null
//    }
//
//    companion object {
//        private val LOG = Logger.getInstance(DirectoryIndexImpl::class.java)
//
//        fun shouldResetOnEvents(events: List<VFileEvent>): Boolean {
//            for (event in events) {
//                // VFileCreateEvent.getFile() is expensive
//                if (event is VFileCreateEvent) {
//                    if (event.isDirectory) return true
//                } else {
//                    val file = event.file
//                    if (file == null || file.isDirectory) {
//                        return true
//                    }
//                }
//            }
//            return false
//        }
//
//        fun isIgnoredFileCreated(event: VFileEvent): Boolean {
//            return event is VFileMoveEvent && FileTypeRegistry.getInstance().isFileIgnored(event.newParent) ||
//                    event is VFilePropertyChangeEvent &&
//                    event.propertyName == VirtualFile.PROP_NAME &&
//                    FileTypeRegistry.getInstance().isFileIgnored(event.file)
//        }
//    }
//}
