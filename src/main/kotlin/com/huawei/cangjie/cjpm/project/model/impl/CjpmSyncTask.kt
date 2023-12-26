package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.CjTask
import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class CjpmSyncTask(project: Project) :
    Task.Backgroundable(project, CangJieBundle.message("progress.title.reloading.cjpm.projects"), true),
    CjTask {


    private fun doRun(
        indicator: ProgressIndicator,
        syncProgress: BuildProgress<BuildProgressDescriptor>
    ) {

    }

    override fun run(indicator: ProgressIndicator) {
        LOG.info("CargoSyncTask started")
        indicator.isIndeterminate = true
        val start = System.currentTimeMillis()

        val syncProgress = SyncViewManager.createBuildProgress(project)


    }

    override val taskType: CjTask.TaskType
        get() = CjTask.TaskType.CJPM_SYNC

    override val runSyncInUnitTests: Boolean
        get() = true

    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor = BuildContentDescriptor(
            null,
            null,
            object : JComponent() {},
            CangJieBundle.message("build.event.title.cjpm")
        )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false
//        buildContentDescriptor.isNavigateToError =
        val refreshAction = ActionManager.getInstance().getAction("Cargo.RefreshCargoProject")
        val descriptor = DefaultBuildDescriptor(
            Any(),
            CangJieBundle.message("build.event.title.cjpm"),
            project.basePath!!,
            System.currentTimeMillis()
        )
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(refreshAction)
            .withRestartAction(StopAction(progress))
        return object : BuildProgressDescriptor {
            override fun getTitle(): String = descriptor.title
            override fun getBuildDescriptor(): BuildDescriptor = descriptor
        }
    }

    private class StopAction(private val progress: ProgressIndicator) :
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun actionPerformed(e: AnActionEvent) {
            progress.cancel()
        }
    }


    companion object {
        private val LOG = logger<CjpmSyncTask>()
    }
}