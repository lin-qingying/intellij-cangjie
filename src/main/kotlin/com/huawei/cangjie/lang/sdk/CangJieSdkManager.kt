package com.huawei.cangjie.lang.sdk

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.ProjectManagerScope

object CangJieSdkManager {

    val projectManager = ProjectManager.getInstance()

    /**
     * 获取项目使用sdk
     */
    fun getProjectSdk(): Sdk? {

        val openProjects = projectManager.openProjects
        if (openProjects.isNotEmpty()) {
            val project = openProjects[0]
            val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return null
            return if (sdk.sdkType is CangJieSdkType) sdk else null
        }
        return null


    }

    /**
     * 获取所有仓颉sdk
     */
    fun getAllCangJieSdks(): List<CangJieSdk> {

        val allsdk = ProjectJdkTable.getInstance().allJdks.toList()
        return allsdk.filterIsInstance<CangJieSdk>()


    }
}
