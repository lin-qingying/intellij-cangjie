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

package cn.cangnova.cangjie.cjpm.project.model.impl

import cn.cangnova.cangjie.cjpm.project.model.CjcInfo
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.workspace.CjpmWorkspace
import cn.cangnova.cangjie.cjpm.project.workspace.StandardLibrary
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject.UpdateStatus
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject.UpdateStatus.NeedsUpdate
import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

// 获取是否启用新项目模型导入的值
val isNewProjectModelImportEnabled: Boolean
    get() = Registry.`is`("cn.cangnova.cangjie.cjpm.new.auto.import", false)


// 定义CjpmProjectImpl类，实现CjpmProject接口
data class CjpmProjectImpl(
    override val manifest: Path,
    private val projectService: CjpmProjectsServiceImpl,
    override val userDisabledFeatures: UserDisabledFeatures = UserDisabledFeatures.EMPTY,

    val rawWorkspace: CjpmWorkspace? = null,
    private val stdlib: StandardLibrary? = null,

    override val cjcInfo: CjcInfo? = null,
    override val workspaceStatus: UpdateStatus = NeedsUpdate,
    override val stdlibStatus: UpdateStatus = NeedsUpdate,
    override val cjcInfoStatus: UpdateStatus = NeedsUpdate
) : UserDataHolderBase(), CjpmProject {
    //    override val workspaceRootDir: VirtualFile? = project.baseDir
    // 获取工作区根目录
    override val workspaceRootDir: VirtualFile?
        get() = rawWorkspace?.workspaceRoot

    private val rootDirCache = AtomicReference<VirtualFile>()


    // 判断是否是工作区
    override val isWorkspace: Boolean
        get() {
            return rawWorkspace != null
        }

    // 获取根目录
    override val rootDir: VirtualFile?
        get() {
            val cached = rootDirCache.get()
            if (cached != null && cached.isValid) return cached
            val file = LocalFileSystem.getInstance().findFileByIoFile(workingDirectory.toFile())
            rootDirCache.set(file)
            return file
        }

    // 获取项目
    override val project: Project
        get() = projectService.project

    // 获取工作区
    override val workspace: CjpmWorkspace? by lazy(LazyThreadSafetyMode.PUBLICATION) {

//        val rawWorkspace = rawWorkspace ?: return@lazy null
//
//        val stdlib = stdlib ?: return@lazy rawWorkspace
//        val stdlib = stdlib ?: return@lazy if (!userDisabledFeatures.isEmpty() && isUnitTestMode) {
//            rawWorkspace.withDisabledFeatures(userDisabledFeatures)
//        } else {
//            rawWorkspace
//        }
//        rawWorkspace.withStdlib(stdlib, cjcInfo)
        val rawWorkspace = rawWorkspace ?: return@lazy null
        val stdlib = stdlib ?: return@lazy rawWorkspace/*if (!userDisabledFeatures.isEmpty() && isUnitTestMode) {
            rawWorkspace.withDisabledFeatures(userDisabledFeatures)
        } else {
            rawWorkspace
        }*/
        rawWorkspace.withStdlib(stdlib, cjcInfo)
//            .withDisabledFeatures(userDisabledFeatures)

    }

    // 设置标准库
    fun withStdlib(result: TaskResult<StandardLibrary>): CjpmProjectImpl =
        if (!CangJieLanguageServerServices.getInstance().astConfig.enabled) {
            copy(stdlib = null, stdlibStatus = UpdateStatus.UpToDate)
        } else {
            when (result) {
                is TaskResult.Ok -> copy(stdlib = result.value, stdlibStatus = UpdateStatus.UpToDate)
                is TaskResult.Err -> copy(stdlibStatus = UpdateStatus.UpdateFailed(result.reason))
            }
        }


    // 获取可呈现的名称
    override val presentableName: String
        get() {
            return workingDirectory.fileName.toString()
        }

    fun withWorkspace(result: TaskResult<CjpmWorkspace>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(
            rawWorkspace = result.value,
            workspaceStatus = UpdateStatus.UpToDate,
//            userDisabledFeatures = userDisabledFeatures.retain(result.value.packages)
        )

        is TaskResult.Err -> copy(workspaceStatus = UpdateStatus.UpdateFailed(result.reason))
    }

    fun withCjcInfo(result: TaskResult<CjcInfo>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(cjcInfo = result.value, cjcInfoStatus = UpdateStatus.UpToDate)
        is TaskResult.Err -> copy(cjcInfoStatus = UpdateStatus.UpdateFailed(result.reason))
    }
    // 设置工作区
//    fun withWorkspace(result: TaskResult<CjpmWorkspace>): CjpmProjectImpl = when (result) {
//        is TaskResult.Ok -> copy(stdlib = null,
//            rawWorkspace = result.value,
//            workspaceStatus = CjpmProject.UpdateStatus.UpToDate,
////            userDisabledFeatures = userDisabledFeatures.retain(result.value.packages)
//        )
//
//        is TaskResult.Err -> copy(workspaceStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
//    }
//
//    fun withCjcInfo(result: TaskResult<CjcInfo>): CjpmProjectImpl = when (result) {
//        is TaskResult.Ok -> copy(cjcInfo = result.value, stdlibStatus = NeedsUpdate, stdlib = null, cjcInfoStatus = CjpmProject.UpdateStatus.UpToDate)
//        is TaskResult.Err -> copy(stdlib = null, stdlibStatus = NeedsUpdate,cjcInfoStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
//    }
}

val CjpmProject.workingDirectory: Path get() = manifest.parent


class CachedVirtualFile(private val url: String?) {
    // 使用AtomicReference来缓存VirtualFile对象
    private val cache = AtomicReference<VirtualFile>()

    // 通过操作符重载，实现属性的getter方法
    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
        // 如果url为空，则返回null
        if (url == null) return null
        // 获取缓存中的VirtualFile对象
        val cached = cache.get()
        // 如果缓存中的对象不为空且有效，则返回缓存中的对象
        if (cached != null && cached.isValid) return cached
        // 根据url查找VirtualFile对象
        val file = VirtualFileManager.getInstance().findFileByUrl(url)
        // 将查找到的对象设置到缓存中
        cache.set(file)
        // 返回查找到的对象
        return file
    }
}
