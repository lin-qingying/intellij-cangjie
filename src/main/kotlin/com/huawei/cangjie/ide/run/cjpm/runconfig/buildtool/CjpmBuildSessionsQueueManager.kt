package com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool

import com.huawei.cangjie.CangJieBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.project.Project


@Service
class CjpmBuildSessionsQueueManager(project: Project) {
    val buildSessionsQueue: BackgroundTaskQueue = BackgroundTaskQueue(project, CangJieBundle.message("progress.title.building"))

    companion object {
        fun getInstance(project: Project): CjpmBuildSessionsQueueManager = project.service()
    }
}
