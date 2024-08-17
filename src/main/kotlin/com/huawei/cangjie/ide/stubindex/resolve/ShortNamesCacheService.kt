package com.huawei.cangjie.ide.stubindex.resolve

import com.huawei.cangjie.ide.vfilefinder.CangJieShortClassNameFileIndex
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import java.util.concurrent.ConcurrentHashMap


@Service(Service.Level.PROJECT)
class ShortNamesCacheService(private val project: Project) {

    private val tracker = FileBaseIndexModificationTracker(CangJieShortClassNameFileIndex.NAME, project)

    private val shortNamesCache =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(ConcurrentHashMap<String, Set<String>>(), tracker)
            },
            /* trackValue = */ false
        )

    fun getShortNameCandidates(name: String): Set<String> =
        shortNamesCache.value.getOrPut(name) {
            val scope = GlobalSearchScope.everythingScope(project)
            val fqNames = hashSetOf<String>()
            FileBasedIndex.getInstance().processValues(
                CangJieShortClassNameFileIndex.NAME, name, null,
                { _, names ->
                    fqNames += names
                    true
                }, scope
            )
            fqNames
        }

    companion object {
        fun getInstance(project: Project): ShortNamesCacheService = project.service()
    }
}

internal class FileBaseIndexModificationTracker<K, V>(private val id: ID<K, V>, private val project: Project) :
    ModificationTracker {
    override fun getModificationCount(): Long =
        FileBasedIndex.getInstance().getIndexModificationStamp(id, project)

}
