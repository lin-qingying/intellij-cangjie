/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.stubindex.resolve

import org.cangnova.cangjie.ide.stubindex.CangJieShortClassNameFileIndex
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
