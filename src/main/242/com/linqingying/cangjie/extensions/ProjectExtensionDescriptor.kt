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

package com.linqingying.cangjie.extensions

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

open class ProjectExtensionDescriptor<T : Any>(name: String, private val extensionClass: Class<T>) {
    private val extensionPointName: ExtensionPointName<T> = ExtensionPointName.create(name)

    fun registerExtensionPoint(project: Project) {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName.name,
            extensionClass
        )
    }

    fun registerExtension(project: Project, extension: T) {

//        project.extensionArea.getExtensionPoint(extensionPointName).registerExtension(extension, project)
        extensionPointName.point.registerExtension(extension, project)
    }

    /**
     * 获取项目中的扩展实例列表
     *
     * 此函数旨在从给定项目中获取特定扩展点的所有扩展实例列表
     * 它首先检查项目中是否存在指定的扩展点，如果不存在，则返回一个空列表
     * 如果存在，它将获取该扩展点下的所有扩展，并将其转换为列表返回
     *
     * @param project 项目实例，用于获取扩展信息
     * @return 返回扩展实例的列表如果指定的扩展点不存在，则返回空列表
     */
    fun getInstances(project: Project): List<T> {
//        // 获取项目的扩展区域
//        val projectArea = project.extensionArea
//        // 检查项目中是否存在指定的扩展点
//        if (!projectArea.hasExtensionPoint(extensionPointName.name)) return listOf()
//
//        // 获取指定扩展点下的所有扩展，并转换为列表返回
//        return projectArea.getExtensionPoint(extensionPointName).extensions.toList()

     return   extensionPointName.extensionList
    }

}
