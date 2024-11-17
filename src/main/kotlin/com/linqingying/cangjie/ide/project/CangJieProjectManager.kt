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

package com.linqingying.cangjie.ide.project

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
