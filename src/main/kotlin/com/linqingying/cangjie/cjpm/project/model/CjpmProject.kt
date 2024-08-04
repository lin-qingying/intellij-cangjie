package com.linqingying.cangjie.cjpm.project.model

import com.linqingying.cangjie.cjpm.project.model.impl.UserDisabledFeatures
import com.linqingying.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

interface CjpmProject : UserDataHolderEx {

    val rootDir: VirtualFile?

    val project: Project
    val workspace: CjpmWorkspace?

    val workspaceRootDir: VirtualFile?
    val cjcInfo: CjcInfo?
    val presentableName: String

    val manifest: Path
    val userDisabledFeatures: UserDisabledFeatures

    //    val workspace: CjpmWorkspace?

    val workspaceStatus: UpdateStatus
    val stdlibStatus: UpdateStatus

    val cjcInfoStatus: UpdateStatus



    val isWorkspace: Boolean

    val mergedStatus: UpdateStatus
        get() = workspaceStatus
            .merge(stdlibStatus)
            .merge(cjcInfoStatus)

    sealed class UpdateStatus(private val priority: Int) {
        object UpToDate : UpdateStatus(0)
        object NeedsUpdate : UpdateStatus(1)
        class UpdateFailed(@Suppress("UnstableApiUsage") @NlsContexts.Tooltip val reason: String) : UpdateStatus(2) {
            override fun toString(): String = reason
        }

        fun merge(status: UpdateStatus): UpdateStatus = if (priority >= status.priority) this else status
    }
}
