/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.model.CjProject

/**
 * 仓颉项目管理服务接口
 *
 * 提供项目的发现、创建、缓存和管理功能
 */
@Service(Service.Level.PROJECT)
interface CjProjectsService {
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CjProjectsService {
            return project.getService(CjProjectsService::class.java)
        }
    }

    /**
     * 获取所有仓颉项目
     */
    val allProjects: List<CjProject>

    /**
     * 根据根目录查找项目
     *
     * @param rootDir 项目根目录
     * @return 找到的项目，如果不存在返回 null
     */
    fun findProject(rootDir: VirtualFile): CjProject?

    /**
     * 根据名称查找项目
     *
     * @param name 项目名称
     * @return 找到的项目，如果不存在返回 null
     */
    fun findProjectByName(name: String): CjProject?

    /**
     * 发现并创建项目
     *
     * 通过扩展点机制尝试识别并创建项目
     *
     * @param rootDir 项目根目录
     * @return 创建的项目，如果无法识别返回 null
     */
    fun discoverProject(rootDir: VirtualFile): CjProject?

    /**
     * 添加项目到缓存
     *
     * @param project 要添加的项目
     */
    fun addProject(project: CjProject)

    /**
     * 从缓存中移除项目
     *
     * @param project 要移除的项目
     */
    fun removeProject(project: CjProject)

    /**
     * 刷新所有项目
     */
    fun refreshAllProjects()

    /**
     * 刷新指定项目
     *
     * @param project 要刷新的项目
     */
    fun refreshProject(project: CjProject)
}