package com.huawei.cangjie.lang.sdk

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.ProjectManagerScope

object CangJieSdkManager {
    private val logger = Logger.getInstance(CangJieSdkManager::class.java)


    /**
     * 获取项目使用sdk
     */
    fun getProjectSdk(): Sdk? {
        val projectManager = ProjectManager.getInstance()

        val openProjects = projectManager.openProjects
        logger.info(openProjects.toString())
        if (openProjects.isNotEmpty()) {
            val project = openProjects[0]
            val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return null
            return if (sdk.sdkType is CangJieSdkType) sdk else null
        }
        return null


    }

    fun getProjectSdk(project:Project):Sdk?{
        val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return null
        return if (sdk.sdkType is CangJieSdkType) sdk else null
    }

    /**
     * 获取所有仓颉sdk
     */
    fun getAllCangJieSdks(): List<Sdk> {
//        返回Sdktype为CangJieSdkType的sdk
//        return ProjectJdkTable.getInstance().getSdksOfType(CangJieSdkType())

        val sdks = ProjectJdkTable.getInstance().allJdks
        val cangJieSdks = mutableListOf<Sdk>()
        for (sdk in sdks) {
            if (sdk.sdkType is CangJieSdkType) {
                cangJieSdks.add(sdk)
            }
        }
        return cangJieSdks


    }
}
