package com.linqingying.cangjie.icon

import com.linqingying.cangjie.cjpm.CjpmConstants

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon


class CjpmIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? = when (file.name) {
        in CjpmConstants.MANIFEST_FILE -> CjpmIcons.ICON
//
        in CjpmConstants.LOCK_FILE -> CjpmIcons.LOCK_ICON
//        CjpmConstantsService(project).MANIFEST_FILE -> CjpmIcons.MANIFEST_ICON
//        CjpmConstantsService(project).LOCK_FILE -> CjpmIcons.LOCK_ICON
        else -> null
    }
}
