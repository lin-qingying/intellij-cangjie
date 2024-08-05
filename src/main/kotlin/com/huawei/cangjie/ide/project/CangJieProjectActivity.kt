package com.huawei.cangjie.ide.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

//class CangJieProjectActivity : ProjectActivity {
//    override suspend fun execute(project: Project) {
//        CangJieProjectManager.setProject(project)
////        val service = project.getService(CangJieService::class.java)
////        service. project = project
//    }
//}


@Service
class CangJieService   {

    var project: Project? = null




}
