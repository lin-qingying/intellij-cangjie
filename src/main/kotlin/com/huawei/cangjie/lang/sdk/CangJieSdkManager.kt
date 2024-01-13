package com.huawei.cangjie.lang.sdk


import com.huawei.cangjie.idea.project.CangJieProjectManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import kotlinx.html.MAIN

object CangJieSdkManager {
    private val logger = Logger.getInstance(CangJieSdkManager::class.java)


    val sdkPath get() = getProjectSdk()?.homePath ?: ""

    val sdkVersion get() = getProjectSdk()?.versionString ?: ""
    fun getProjectSdkType(): CangJieSdkType? {
        return getProjectSdk()?.sdkType as? CangJieSdkType
    }

    /**
     * 获取项目使用sdk
     */
    fun getProjectSdk(): Sdk? {

        val project = CangJieProjectManager.getProject()
        val sdk = project?.let { ProjectRootManager.getInstance(it).projectSdk } ?: return null
//        logger.info(sdk.name)
//        logger.info(sdk.sdkType.toString())
        return if (sdk.sdkType is CangJieSdkType) sdk else null


    }

    fun getProjectSdk(project: Project): Sdk? {


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
