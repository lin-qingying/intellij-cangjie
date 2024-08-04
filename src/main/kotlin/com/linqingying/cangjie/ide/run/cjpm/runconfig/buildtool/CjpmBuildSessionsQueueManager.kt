package com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool

import com.linqingying.cangjie.CangJieBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock



@Service(Service.Level.PROJECT)
class CjpmBuildSessionsQueueManager(project: Project) {
    val buildSessionsQueue: BackgroundTaskQueue =
        BackgroundTaskQueue(project, CangJieBundle.message("progress.title.building"))


    companion object {
        fun getInstance(project: Project): CjpmBuildSessionsQueueManager = project.service()
    }
}
