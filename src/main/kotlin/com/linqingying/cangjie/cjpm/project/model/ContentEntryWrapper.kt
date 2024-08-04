package com.linqingying.cangjie.cjpm.project.model

import com.linqingying.cangjie.cjpm.CjpmConstants
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.vfs.VirtualFile

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

fun ContentEntryWrapper.setup(contentRoot: VirtualFile) {
    val makeVfsUrl = { dirName: String -> contentRoot.findChild(dirName)?.url }
    CjpmConstants.ProjectLayout.sources.mapNotNull(makeVfsUrl).forEach {
        addSourceFolder(it, isTestSource = false)
    }
    CjpmConstants.ProjectLayout.tests.mapNotNull(makeVfsUrl).forEach {
        addSourceFolder(it, isTestSource = true)
    }
    makeVfsUrl(CjpmConstants.ProjectLayout.target)?.let(::addExcludeFolder)
}
