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

package com.linqingying.cangjie.cjpm.project.workspace

import CjpmWorkspaceData
import com.fasterxml.jackson.core.JacksonException
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.model.CjcInfo
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectInfo
import com.linqingying.cangjie.cjpm.project.model.Require
import com.linqingying.cangjie.cjpm.project.model.impl.CachedVirtualFile
import com.linqingying.cangjie.cjpm.project.pathAsPath
import com.linqingying.cangjie.cjpm.resolve
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.linqingying.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import com.linqingying.cangjie.cjpm.project.model.toml.CjpmTomlParser
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists


/**
 * CjpmWorkspace接口定义了一个CJPM项目的workspace所需的基本结构和操作
 * 它提供了对项目manifest路径、内容根目录、workspace根目录的访问，
 * 以及对项目中包含的包的管理
 */
interface CjpmWorkspace {
    /**
     * manifestPath属性返回项目manifest文件的路径
     */
    val manifestPath: Path

    /**
     * contentRoot属性返回项目内容的根目录路径，即manifest文件的父目录
     */
    val contentRoot: Path get() = manifestPath.parent


    val workspaceRoot: VirtualFile?


    /**
     * packages属性返回项目中包含的所有包的集合
     */
    val packages: Collection<Package>

    /**
     * withStdlib函数允许在当前workspace中添加标准库信息
     * @param stdlib 标准库信息
     * @param rustcInfo 可选的rustc信息
     * @return 返回一个新的包含标准库信息的CjpmWorkspace实例
     */
    fun withStdlib(stdlib: StandardLibrary, rustcInfo: CjcInfo? = null): CjpmWorkspace


    //    cjpm module.json数据
//    val moduleData: CjpmProjectInfo?

    //cjpm.toml元数据
    val metadata: CjpmTomlConfig?

    /**
     * 该接口表示一个cjpm包  一个项目会有N个cjpm包
     */
    interface Package : UserDataHolderEx {

        /**
         * contentRoot属性返回包的内容根目录
         */
        val contentRoot: VirtualFile?

        /**
         * rootDirectory属性返回包的根目录路径
         */
        val rootDirectory: Path

        /**
         * name属性返回包的名称
         */
        val name: String

//        val features: Set<PackageFeature>

        /**
         * workspace属性返回包所属的CjpmWorkspace
         */
        val workspace: CjpmWorkspace

        /**
         * manifestPath属性返回包的manifest文件
         */
        val manifestPath: VirtualFile?

        /**
         * moduleData属性返回包的CJPM项目信息
         */
//        val moduleData: CjpmProjectInfo?
        val metadata: CjpmTomlConfig?

        /**
         * version属性返回包的版本号
         */
        val version: String

        /**
         * outDir属性返回包的输出目录
         */
        val outDir: VirtualFile?

        /**
         * origin属性返回包的来源
         */
        val origin: PackageOrigin


//        fun getDependencies(): List<Package>

    }

    /**
     * 伴生对象提供了反序列化CjpmWorkspace实例的方法
     */
    companion object {
        /**
         * deserialize函数用于从给定的manifest路径和数据反序列化一个CjpmWorkspace实例
         * @param manifestPath 项目manifest文件的路径
         * @param data 包含项目信息的数据对象
         * @return 返回反序列化后的CjpmWorkspace实例
         */
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,
        ): CjpmWorkspace = WorkspaceImpl.deserialize(manifestPath, data)
    }
}

/**
 * 表示工作空间中的一个具体的包实现。
 * 包是相关类和接口的集合，用于组织和管理软件模块。
 *
 * @param workspace 该包所属的工作空间，表示包所在的环境。
 * @param contentRootUrl 包的内容根目录URL，指向包的主要资源位置。
 * @param name 包的名称。
 * @param version 包的版本号。
 * @param origin 包的来源信息。
 * @param outDirUrl 输出目录的URL，默认为null。
 * @param moduleData 包含项目信息的模块数据，默认为null。
 */
class PackageImpl(
    override val workspace: WorkspaceImpl,
    val contentRootUrl: String,
    override val name: String,
    override val version: String,

    override var origin: PackageOrigin,
    val outDirUrl: String? = null,

//    override var moduleData: CjpmProjectInfo? = null,
    override val metadata: CjpmTomlConfig? = null

) : UserDataHolderBase(), CjpmWorkspace.Package {

    /**
     * 返回包的字符串表示形式，包含名称、内容根目录URL和版本号。
     */
    override fun toString() = "Package(name='$name', contentRootUrl='$contentRootUrl', version='$version')"

    /**
     * 获取缓存的内容根目录虚拟文件。
     */
    override val contentRoot: VirtualFile? by CachedVirtualFile(contentRootUrl)

    /**
     * 获取清单文件（manifest）的虚拟文件路径。
     */
    override val manifestPath: VirtualFile? = contentRoot?.pathAsPath?.resolve(CjpmConstants.MANIFEST_FILE)?.let {
        LocalFileSystem.getInstance().findFileByNioFile(it)
    }

    // TODO 为每个包的module.json编制数据 时间占用过于庞大，目前用不到，所以就先不读了
    // init {
    //     moduleData = if (manifestPath?.exists() == true) {
    //
    //         val json = manifestPath.toFile().readText()
    //         try {
    //             Cjpm.JSON_MAPPER.readValue(json, CjpmProjectInfo::class.java)
    //         } catch (e: JacksonException) {
    //             throw e
    //             println(e)
    //             null
    //         }
    //     } else {
    //         null
    //     }
    // }

    /**
     * 根据给定的路径获取内容根目录的虚拟文件。
     *
     * @receiver Require 类型的对象，通常表示依赖项。
     * @return 如果路径存在，则返回对应的虚拟文件；否则在当前工作空间中查找同名包的内容根目录虚拟文件。
     */
    fun Require.contentRoot(): VirtualFile? = if (path != null) {
        LocalFileSystem.getInstance().findFileByPath(path)
    } else {
        workspace.packages.find { `package` ->
            name == `package`.name
        }?.contentRoot
    }

    /**
     * 获取包的根目录路径。
     */
    override val rootDirectory: Path
        get() = Paths.get(VirtualFileManager.extractPath(contentRootUrl))

    /**
     * 获取缓存的输出目录虚拟文件。
     */
    override val outDir: VirtualFile? by CachedVirtualFile(outDirUrl)
}


class WorkspaceImpl(
    override val manifestPath: Path,
    val workspaceRootUrl: String?,
    packagesData: Collection<CjpmWorkspaceData.Package>,
) : CjpmWorkspace {


    override val workspaceRoot: VirtualFile? by CachedVirtualFile(workspaceRootUrl)

    override val metadata: CjpmTomlConfig? = if (manifestPath.exists()) {
        try {
            CjpmTomlParser.parse(manifestPath)
        } catch (e: JacksonException) {
            throw e
            println(e)
            null
        }
    } else {
        null
    }
    override val packages: Collection<PackageImpl> = packagesData.map {

        PackageImpl(
            this,
            it.contentRootUrl,
            it.name,
            it.version,
            it.origin,
        )

    }.toMutableList().apply {
        metadata?.`package`?.let {
            add(
                PackageImpl(
                    this@WorkspaceImpl,
                    this@WorkspaceImpl.workspaceRootUrl ?: "",
                    it.name,
                    it.version,
                    PackageOrigin.WORKSPACE,
                    metadata = metadata,
                )
            )
        }

        if (metadata?.workspace != null) {
            metadata.workspace.members.forEach {
//                构建配置文件路径
                val path = manifestPath.resolve("..").resolve(it).resolve(CjpmConstants.MANIFEST_FILE)
                if (!path.exists()) throw IllegalArgumentException("Package $it not found")
                val packageData = CjpmTomlParser.parse(path)
                assert(packageData.`package` != null) {
                    "Package $it not found"
                }

                add(
                    PackageImpl(
                        this@WorkspaceImpl,
                        this@WorkspaceImpl.workspaceRootUrl?.plus("/$it") ?: "",
                        packageData.`package`?.name ?: error("Name not found"),
                        packageData.`package`.version,
                        PackageOrigin.WORKSPACE,
                        metadata = packageData


                    )
                )
            }
        }
//        add(
//            PackageImpl(
//                this@WorkspaceImpl,
//                this@WorkspaceImpl.workspaceRootUrl ?: "",
//                moduleData?.name ?: "",
//                moduleData?.version ?: "",
//                PackageOrigin.WORKSPACE,
//
//                moduleData = moduleData
//
//            )
//        )

    }.distinctBy { it.name } // 根据name去重


    override fun withStdlib(stdlib: StandardLibrary, rustcInfo: CjcInfo?): CjpmWorkspace {
//        TODO 添加标准库
        val (newPackagesData, @Suppress("NAME_SHADOWING") stdlib) = Pair(packages.map { it.asPackageData() } + stdlib.packages,
            stdlib)

        val result = WorkspaceImpl(
            manifestPath,
            workspaceRootUrl,
            newPackagesData,
//            cfgOptions,
//            cjpmConfig,
//            featuresState
        )
        return result

    }


    companion object {
        fun deserialize(
            manifestPath: Path,
            data: CjpmWorkspaceData,

            ): WorkspaceImpl {
            val result = WorkspaceImpl(
                manifestPath,
                data.workspaceRootUrl,
                data.packages,


                )

            return result

        }
    }
}


private fun PackageImpl.asPackageData(): CjpmWorkspaceData.Package = CjpmWorkspaceData.Package(

    contentRootUrl = contentRootUrl,
    name = name,
    version = version,
    origin = origin,


    )

fun CjpmWorkspace.Package.additionalRoots(): List<VirtualFile> {
    return emptyList()

}
