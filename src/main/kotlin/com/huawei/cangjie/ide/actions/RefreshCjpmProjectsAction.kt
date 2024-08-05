package com.huawei.cangjie.ide.actions

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.model.CjpmProjectActionBase
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import com.huawei.cangjie.cjpm.project.toolwindow.hasCjpmProject
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.saveAllDocuments
import com.huawei.cangjie.ide.run.cjpm.toolchain
import com.intellij.ide.IdeBundle
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class RefreshCjpmProjectsAction : CjpmProjectActionBase() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && project.toolchain != null && project.hasCjpmProject
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!project.confirmLoadingUntrustedProject()) return

        saveAllDocuments()
        if (project.toolchain == null || !project.hasCjpmProject) {
            guessAndSetupCangJieProject(project, explicitRequest = true)
        } else {
            project.cjpmProjects.refreshAllProjects()
        }
    }
}

@Suppress("UnstableApiUsage")
fun Project.confirmLoadingUntrustedProject(): Boolean {
    return isTrusted() || com.intellij.ide.impl.confirmLoadingUntrustedProject(
        this,
        title = IdeBundle.message("untrusted.project.dialog.title", CangJieBundle.message("cjpm"), 1),
        message = IdeBundle.message("untrusted.project.dialog.text", CangJieBundle.message("cjpm"), 1),
        trustButtonText = IdeBundle.message("untrusted.project.dialog.trust.button"),
        distrustButtonText = IdeBundle.message("untrusted.project.dialog.distrust.button")
    )
}
