package com.huawei.cangjie.idea.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

//interface CjpmProjectsService {
//
//    val project: Project
//    val allProjects: Collection<CjpmProject>
//    fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>>
//    fun findProjectForFile(file: VirtualFile): CjpmProject?
//    val initialized: Boolean
//    companion object {
//        val CJPM_PROJECTS_TOPIC: Topic<CjpmProjectsListener> = Topic(
//            "cargo projects changes",
//            CjpmProjectsListener::class.java
//        )
//
//        val CJPM_PROJECTS_REFRESH_TOPIC: Topic<CjpmProjectsRefreshListener> = Topic(
//            "Cargo refresh",
//            CjpmProjectsRefreshListener::class.java
//        )
//    }
//
//    enum class CjpmRefreshStatus {
//        SUCCESS,
//        FAILURE,
//        CANCEL
//    }
//    interface CjpmProjectsRefreshListener {
//        fun onRefreshStarted()
//        fun onRefreshFinished(status: CjpmRefreshStatus)
//    }
//    fun interface CjpmProjectsListener {
//        fun cjpmProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>)
//    }
//}

//interface CjpmProject : UserDataHolderEx {
//    val presentableName: String
//    val project: Project
//    val manifest: Path
//   val workingDirectory: Path get() = manifest.parent
//
//    val workspaceRootDir: VirtualFile?
//}
