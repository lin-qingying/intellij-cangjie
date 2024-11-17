/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

//package com.linqingying.cangjie.lang.sdk
//
//
//import com.linqingying.cangjie.ide.project.CangJieProjectManager
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.project.ProjectManager
//import com.intellij.openapi.projectRoots.ProjectJdkTable
//import com.intellij.openapi.projectRoots.Sdk
//import com.intellij.openapi.roots.ProjectRootManager
//import kotlinx.html.MAIN
//
//object CangJieSdkManager {
//    private val logger = Logger.getInstance(CangJieSdkManager::class.java)
//
//
//    val sdkPath get() = getProjectSdk()?.homePath ?: ""
//
//    val sdkVersion get( ) = getProjectSdk()?.versionString ?: ""
//
//    fun getProjectSdkType(): CangJieSdkType? {
//        return getProjectSdk()?.sdkType as? CangJieSdkType
//    }
//
//    /**
//     * 获取项目使用sdk
//     */
//    fun getProjectSdk(): Sdk? {
//
//        val project = CangJieProjectManager.getProject()
//        val sdk = project?.let { ProjectRootManager.getInstance(it).projectSdk } ?: return null
////        logger.info(sdk.name)
////        logger.info(sdk.sdkType.toString())
//        return if (sdk.sdkType is CangJieSdkType) sdk else null
//
//
//    }
//
//    fun getProjectSdk(project: Project): Sdk? {
//
//
//        val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return null
//        return if (sdk.sdkType is CangJieSdkType) sdk else null
//    }
//
//    /**
//     * 获取所有仓颉sdk
//     */
//    fun getAllCangJieSdks(): List<Sdk> {
////        返回Sdktype为CangJieSdkType的sdk
////        return ProjectJdkTable.getInstance().getSdksOfType(CangJieSdkType())
//
//        val sdks = ProjectJdkTable.getInstance().allJdks
//        val cangJieSdks = mutableListOf<Sdk>()
//        for (sdk in sdks) {
//            if (sdk.sdkType is CangJieSdkType) {
//                cangJieSdks.add(sdk)
//            }
//        }
//        return cangJieSdks
//
//
//    }
//}
