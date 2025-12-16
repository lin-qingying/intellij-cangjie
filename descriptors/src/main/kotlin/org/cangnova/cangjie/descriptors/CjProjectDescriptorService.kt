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

package org.cangnova.cangjie.descriptors

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.descriptors.impl.ProjectDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.LockBasedStorageManager

/**
 * 仓颉项目描述符服务接口
 *
 * 这是一个项目级服务接口，用于管理和提供项目的 [ProjectDescriptor] 实例。
 * 每个 IntelliJ Project 都有一个对应的 ProjectDescriptor 实例。
 *
 * ## 特性
 *
 * - **单例保证**: 每个项目只有一个 ProjectDescriptor 实例
 * - **延迟初始化**: 只在首次访问时创建
 * - **线程安全**: 使用 lazy 委托确保线程安全
 * - **生命周期管理**: 与 IntelliJ Project 生命周期绑定
 *
 * ## 使用方式
 *
 * ```kotlin
 * val projectDescriptor = project.service<CjProjectDescriptorService>().projectDescriptor
 * ```
 *
 * @see ProjectDescriptor
 * @see ProjectDescriptorImpl
 */
interface CjProjectDescriptorService {
    /**
     * 项目描述符实例
     *
     * 使用 lazy 委托实现延迟初始化和线程安全
     */
    val projectDescriptor: ProjectDescriptor

    /**
     * 使项目描述符失效
     *
     * 调用此方法后，项目描述符将不再可用。
     * 通常在项目关闭或重新加载时调用。
     */
    fun invalidate()

    companion object {
        /**
         * 获取项目描述符服务实例
         *
         * @param project IntelliJ 项目实例
         * @return CjProjectDescriptorService 实例
         */
        @JvmStatic
        fun getInstance(project: Project): CjProjectDescriptorService {
            return project.service()
        }
    }
}

/**
 * 仓颉项目描述符服务实现
 *
 * 这是一个 internal 实现类，不应在模块外部直接使用。
 * 请通过 [CjProjectDescriptorService] 接口访问服务。
 *
 * @see CjProjectDescriptorService
 */
@Service(Service.Level.PROJECT)
internal class CjProjectDescriptorServiceImpl(private val project: Project) : CjProjectDescriptorService {

    /**
     * 项目描述符实例
     *
     * 使用 lazy 委托实现延迟初始化和线程安全
     */
    override val projectDescriptor: ProjectDescriptor by lazy {
        createProjectDescriptor()
    }

    /**
     * 创建项目描述符实例
     *
     * @return 新创建的 ProjectDescriptor 实例
     */
    private fun createProjectDescriptor(): ProjectDescriptor {
        val projectName = Name.identifier(project.name)

        // 创建 StorageManager，参考 GlobalContext 的实现方式
        val tracker = org.cangnova.cangjie.ExceptionTracker()
        val storageManager = LockBasedStorageManager.createWithExceptionHandling(
            "CjProjectDescriptor-${project.name}",
            tracker,
            { ProgressManager.checkCanceled() },
            { throw ProcessCanceledException(it) }
        )

        return ProjectDescriptorImpl(
            projectName = projectName,
            project = project,
            storageManager = storageManager
        )
    }

    /**
     * 使项目描述符失效
     *
     * 调用此方法后，项目描述符将不再可用。
     * 通常在项目关闭或重新加载时调用。
     */
    override fun invalidate() {
        if (projectDescriptor.isValid) {
            (projectDescriptor as? ProjectDescriptorImpl)?.invalidate()
        }
    }
}
