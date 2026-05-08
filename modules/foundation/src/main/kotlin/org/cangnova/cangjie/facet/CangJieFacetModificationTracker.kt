package org.cangnova.cangjie.facet

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.jps.entities.FacetEntity
import com.intellij.platform.workspace.jps.entities.ModuleSettingsFacetBridgeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 仓颉 facet 修改跟踪器。
 *
 * 对位 Kotlin `KotlinFacetModificationTracker`。
 * 仓颉当前没有额外的 facet workspace entity，因此这里只跟踪真实的 facet 挂载变化。
 */
@Service(Service.Level.PROJECT)
class CangJieFacetModificationTracker(
    project: Project,
    coroutineScope: CoroutineScope,
) : SimpleModificationTracker() {
    init {
        coroutineScope.launch {
            WorkspaceModel.getInstance(project).eventLog.collect { event ->
                val facetChanges = event.getChanges(FacetEntity::class.java)
                if (facetChanges.isEmpty()) return@collect

                for (facetChange in facetChanges) {
                    val cangjieFacetEntity = facetChange.oldFacetEntity()?.takeIf { entity -> entity.isCangJieFacet() }
                        ?: facetChange.newFacetEntity()?.takeIf { entity -> entity.isCangJieFacet() }
                    if (cangjieFacetEntity != null) {
                        incModificationCount()
                        return@collect
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CangJieFacetModificationTracker = project.service()
    }
}

private fun EntityChange<FacetEntity>.oldFacetEntity(): FacetEntity? = when (this) {
        is EntityChange.Added -> null
        is EntityChange.Removed -> oldEntity
        is EntityChange.Replaced -> oldEntity
    }

private fun EntityChange<FacetEntity>.newFacetEntity(): FacetEntity? = when (this) {
        is EntityChange.Added -> newEntity
        is EntityChange.Removed -> null
        is EntityChange.Replaced -> newEntity
    }

fun ModuleSettingsFacetBridgeEntity.isCangJieFacet(): Boolean = name == CangJieFacetType.NAME

private fun FacetEntity.isCangJieFacet(): Boolean = name == CangJieFacetType.NAME
