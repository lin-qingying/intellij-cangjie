package com.huawei.cangjie.idea.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

object CangJieProjectManager {


    /**
     * 获取当前的项目
     */
    fun getCurrentProject(): Project {
        return ProjectManager.getInstance().openProjects[0]
    }


}
