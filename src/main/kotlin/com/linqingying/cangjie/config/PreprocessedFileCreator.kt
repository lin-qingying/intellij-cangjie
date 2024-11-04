package com.linqingying.cangjie.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor

/**
 * The interface for the extensions that are used to substitute VirtualFile on the creation of KtFile, allows to preprocess a file before
 * lexing and parsing
 */
interface PreprocessedVirtualFileFactoryExtension {
    companion object : ProjectExtensionDescriptor<PreprocessedVirtualFileFactoryExtension>(
        "com.linqingying.cangjie.preprocessedVirtualFileFactoryExtension",
        PreprocessedVirtualFileFactoryExtension::class.java
    )

    fun isPassThrough(): Boolean

    fun createPreprocessedFile(file: VirtualFile?): VirtualFile?
    fun createPreprocessedLightFile(file: LightVirtualFile?): LightVirtualFile?
}
class PreprocessedFileCreator(val project: Project) {

    private val validExts: Array<PreprocessedVirtualFileFactoryExtension> by lazy {
        PreprocessedVirtualFileFactoryExtension.getInstances(project).filterNot { it.isPassThrough() }.toTypedArray()
    }

    fun create(file: VirtualFile): VirtualFile = validExts.firstNotNullOfOrNull { it.createPreprocessedFile(file) } ?: file

    // unused now, but could be used in the IDE at some point
    fun createLight(file: LightVirtualFile): LightVirtualFile =
        validExts.firstNotNullOfOrNull { it.createPreprocessedLightFile(file) } ?: file
}
