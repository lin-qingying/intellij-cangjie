/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.task

import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.cangjieProjectService
import org.cangnova.cangjie.project.workspace.CjWorkspaceModelSync
import org.cangnova.cangjie.task.CangJieTask
import javax.swing.JComponent

/**
 * 仓颉项目同步任务
 *
 * 该任务是一个带有进度条的任务执行器，负责刷新仓颉项目。
 * 所有从UI/感知层触发的项目刷新都应该通过这个类开始。
 *
 * 实现了CangJieTask接口，可以通过任务队列服务进行管理。
 * 采用单项目模型，每个IntelliJ项目对应一个仓颉项目。
 *
 * @param project IntelliJ 项目实例
 */
class CangJieSyncTask(
    project: Project,

//    刷新完成回调
    val onFinished: ((CjProject) -> Unit)  =   {

    }
) : Task.Backgroundable(project, CjProjectBundle.message("progress.title.reloading.cangjie.project"), true),
    CangJieTask {

    companion object {
        private val LOG = logger<CangJieSyncTask>()
    }

    /**
     * 执行同步任务
     *
     * @param indicator 进度指示器
     */
    override fun run(indicator: ProgressIndicator) {
        LOG.info("CangJieSyncTask started")

        // 设置进度指示器为不确定状态
        indicator.isIndeterminate = true

        // 记录开始时间
        val startTime = System.currentTimeMillis()

        // 创建 BuildProgress 以在 Build 窗口中显示同步进度
        val syncProgress = SyncViewManager.createBuildProgress(project)

        try {
            // 开始同步进度
            syncProgress.start(createSyncProgressDescriptor(indicator))

            // 执行项目同步
            doSync(indicator, syncProgress)

            // 完成同步
            syncProgress.finish()

            // 计算耗时
            val elapsedTime = System.currentTimeMillis() - startTime
            LOG.info("Finished CangJie sync task in $elapsedTime ms")

        } catch (e: ProcessCanceledException) {
            LOG.info("CangJie sync task was cancelled")
            syncProgress.cancel()
            throw e
        } catch (e: Exception) {
//            LOG.error("Error during CangJie sync task", e)
            syncProgress.fail()
            throw e
        }
    }

    /**
     * 执行实际的同步逻辑
     *
     * 直接调用 CjProject.refresh() 执行刷新。
     * 该方法负责进度显示和状态更新。
     *
     * @param indicator 进度指示器
     * @param syncProgress Build 窗口的进度显示
     */
    private fun doSync(indicator: ProgressIndicator, syncProgress: BuildProgress<BuildProgressDescriptor>) {
        val projectService = project.cangjieProjectService

        // 获取当前仓颉项目
        val cjProject = projectService.cjProject

        if (!cjProject.isValid) {
            LOG.info("No CangJie project found to sync")
            indicator.text = CjProjectBundle.message("progress.text.no.projects.found")
            syncProgress.output(CjProjectBundle.message("progress.text.no.projects.found"), true)
            return
        }

        // 检查是否被取消
        indicator.checkCanceled()

        // 更新进度指示器
        indicator.text = CjProjectBundle.message(
            "progress.text.refreshing.project",
            cjProject.name
        )

        LOG.info("Syncing project: ${cjProject.name}")

        // 使用子进度显示项目的刷新过程
        syncProgress.runWithChildProgress(
            CjProjectBundle.message("build.event.title.sync.project", cjProject.name),
            createContext = { it },
            action = { childProgress ->
                try {
                    // 调用项目服务的刷新方法，触发完整的刷新流程
                    // 使用回调确保在所有异步操作完成后才执行后续操作
                    cjProject.refresh {
                        // 刷新完成后,同步到 Workspace Model
                        try {
                            val workspaceSync = projectService.intellijProject.service<CjWorkspaceModelSync>()

                            // 使用 runBlocking 而非 runBlockingCancellable,因为我们在后台线程池中
                            kotlinx.coroutines.runBlocking {
                                workspaceSync.syncProject(cjProject)
                            }

                            LOG.info("Successfully synced workspace model for project: ${cjProject.name}")
                        } catch (e: Exception) {
                            LOG.error("Failed to sync workspace model for project: ${cjProject.name}", e)
                            // 不抛出异常,允许后续操作继续
                        }

                        // 所有操作完成后调用 onFinished
                        onFinished(cjProject)
                        childProgress.finish()

                        // 更新进度显示
                        indicator.fraction = 1.0
                        indicator.text = CjProjectBundle.message(
                            "progress.text.refreshing.completed.single"
                        )
                    }
                } catch (e: ProcessCanceledException) {
                    childProgress.cancel()
                    throw e
                } catch (e: Exception) {
//                    LOG.error("Failed to sync project: ${cjProject.name}", e)
                    childProgress.message(
                        CjProjectBundle.message("build.event.title.failed.to.refresh.project", cjProject.name),
                        e.message ?: CjProjectBundle.message("error.unknown.error"),
                        MessageEvent.Kind.ERROR,
                        null
                    )
                    childProgress.fail()
//                    throw e
                }
            }
        )
    }

    /**
     * 任务类型
     */
    override val taskType: CangJieTask.TaskType
        get() = CangJieTask.TaskType.CANGJIE_SYNC

    /**
     * 在单元测试中同步运行
     */
    override val runSyncInUnitTests: Boolean
        get() = true

    /**
     * 是否等待智能模式
     */
    override val waitForSmartMode: Boolean
        get() = true

    /**
     * 进度条显示延迟
     */
    override val progressBarShowDelay: Int
        get() = 500 // 500ms delay

    /**
     * 创建同步进度描述符
     *
     * 用于在 Build 窗口中显示同步进度。
     *
     * @param progress 进度指示器
     * @return BuildProgressDescriptor 实例
     */
    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor = BuildContentDescriptor(
            null,
            null,
            object : JComponent() {},
            CjProjectBundle.message("build.event.title.cangjie")
        )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false

        val refreshAction = ActionManager.getInstance().getAction("CjProject.RefreshProject")
        val descriptor = DefaultBuildDescriptor(
            Any(),
            CjProjectBundle.message("build.event.title.cangjie"),
            project.basePath!!,
            System.currentTimeMillis()
        )
            .withContentDescriptor { buildContentDescriptor }
            .let { if (refreshAction != null) it.withRestartAction(refreshAction) else it }
            .withRestartAction(StopAction(progress))

        return object : BuildProgressDescriptor {
            override fun getTitle(): String = descriptor.title
            override fun getBuildDescriptor(): BuildDescriptor = descriptor
        }
    }

    /**
     * 停止操作
     *
     * 用于在 Build 窗口中提供停止按钮。
     */
    private class StopAction(private val progress: ProgressIndicator) :
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            progress.cancel()
        }
    }
}

/**
 * BuildProgress 扩展方法：使用子进度执行操作
 *
 * 为每个子任务创建独立的进度显示，并在完成时自动调用 onResult 回调。
 *
 * @param T 上下文类型
 * @param R 结果类型
 * @param title 子进度标题
 * @param createContext 创建上下文的函数
 * @param action 要执行的操作
 * @param onResult 结果回调，默认完成子进度
 * @return 操作结果
 */
private fun <T, R> BuildProgress<BuildProgressDescriptor>.runWithChildProgress(
    @BuildEventsNls.Title title: String,
    createContext: (BuildProgress<BuildProgressDescriptor>) -> T,
    action: (T) -> R,
    onResult: (BuildProgress<BuildProgressDescriptor>, R) -> Unit = { progress, _ -> progress.finish() }
): R {
    val childProgress = startChildProgress(title)
    try {
        val context = createContext(childProgress)
        val result = action(context)
        onResult(childProgress, result)
        return result
    } catch (e: Throwable) {
        if (e is ProcessCanceledException) {
            childProgress.cancel()
        } else {
            childProgress.fail()
        }
        throw e
    }
}
