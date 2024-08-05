package com.huawei.cangjie.ide.project

import com.google.common.util.concurrent.ServiceManager
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import java.nio.file.Path
import java.nio.file.Paths


//object CangJieProjectManager {
//
//    fun basePath(): Path = Paths.get(getProject()?.basePath!!)
//    val workspaceRootDir: VirtualFile? = getProject()?.baseDir
////    val currentProject: Project
////        get() = getCurrentProject()
//
//
//    /**
//     * 获取当前的项目
//     */
//
//    private var project: Project? = null
//
//
//    fun setProject(project: Project) {
//        this.project = project
//    }
//
//    fun getProject(): Project? {
//        return project
//    }
//
//
//    fun getCurrentProject(): Project {
//
////        val service = service<CangJieService>()
////
////        return service.project!!
////
//
////        if (project == null) return DataManager.getInstance()
////            .getDataContext(WindowManager.getInstance().suggestParentWindow(null)).getData(CommonDataKeys.PROJECT)!!
//
//        return getProject()!!
//
//    }
//
//}
