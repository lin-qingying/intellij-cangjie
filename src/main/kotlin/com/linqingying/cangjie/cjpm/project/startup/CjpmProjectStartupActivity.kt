package com.linqingying.cangjie.cjpm.project.startup

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.linqingying.cangjie.cjpm.project.toolwindow.CjpmToolWindow

//class CjpmProjectStartupActivity : ProjectActivity {
//    override suspend fun execute(project: Project) {
//        invokeLater {
//            if (!project.isDisposed) {
//                CjpmToolWindow.initializeToolWindow(project)
//            }
//        }
//    }
//}