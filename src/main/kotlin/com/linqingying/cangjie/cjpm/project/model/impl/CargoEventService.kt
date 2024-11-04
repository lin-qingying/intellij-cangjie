package com.linqingying.cangjie.cjpm.project.model.impl

import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService.Companion.CJPM_PROJECTS_TOPIC
import com.linqingying.cangjie.utils.mapToSet
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


/**
 * 监听module-lock.json 以更新项目结构
 */
@Service
class CjpmEventService(project: Project) {

    private val metadataCallTimestamps: ConcurrentMap<Path, Long> = ConcurrentHashMap()

    init {
        project.messageBus.connect().subscribe(CJPM_PROJECTS_TOPIC,
            CjpmProjectsService.CjpmProjectsListener { _, projects ->
                val projectDirs = projects.mapToSet { it.workingDirectory }
                metadataCallTimestamps.keys.retainAll(projectDirs)
            })
    }

    fun onMetadataCall(projectDirectory: Path) {
        metadataCallTimestamps[projectDirectory] = System.currentTimeMillis()
    }

    fun extractTimestamp(projectDirectory: Path): Long? = metadataCallTimestamps.remove(projectDirectory)

    companion object {
        fun getInstance(project: Project): CjpmEventService = project.service()
    }
}
