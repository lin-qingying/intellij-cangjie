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

package com.linqingying.cangjie.cjpm.project.model

import com.linqingying.cangjie.cjpm.CjpmConstants
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.cjpm.project.model.toml.CjpmTomlConfig

class OrderEntryWrapper(private val orderEntry: OrderEntry) {


}

class ContentEntryWrapper(private val contentEntry: ContentEntry) {
    private val knownFolders: Set<String> = contentEntry.knownFolders()

    fun addExcludeFolder(url: String) {
        if (url in knownFolders) return

        contentEntry.addExcludeFolder(url)
    }

    fun addSourceFolder(url: String, isTestSource: Boolean) {
        if (url in knownFolders) return
        contentEntry.addSourceFolder(url, isTestSource)
    }

    private fun ContentEntry.knownFolders(): Set<String> {
        val knownRoots = sourceFolders.mapTo(hashSetOf()) { it.url }
        knownRoots += excludeFolderUrls
        return knownRoots
    }
}

//添加源文件夹，打开文件
fun ContentEntryWrapper.setup(contentRoot: VirtualFile, metadata: CjpmTomlConfig?) {
    val makeVfsUrl = { dirName: String -> contentRoot.findChild(dirName)?.url }

    metadata?.srcDir?.let {
        makeVfsUrl(it)?.let {
            addSourceFolder(it, isTestSource = false)
        }

    } ?: run {
        CjpmConstants.ProjectLayout.sources.mapNotNull(makeVfsUrl).forEach {

            addSourceFolder(it, isTestSource = false)
        }
    }
    CjpmConstants.ProjectLayout.tests.mapNotNull(makeVfsUrl).forEach {
        addSourceFolder(it, isTestSource = true)
    }


    metadata?.targetDir?.let { makeVfsUrl(it) }?.let(::addExcludeFolder)
        ?: makeVfsUrl(CjpmConstants.ProjectLayout.target)?.let(::addExcludeFolder)
}
