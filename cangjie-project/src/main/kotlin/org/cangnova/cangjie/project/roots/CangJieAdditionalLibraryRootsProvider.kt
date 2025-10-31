///*
// * Copyright 2025 LinQingYing. and contributors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * The use of this source code is governed by the Apache License 2.0,
// * which allows users to freely use, modify, and distribute the code,
// * provided they adhere to the terms of the license.
// *
// * The software is provided "as-is", and the authors are not responsible for
// * any damages or issues arising from its use.
// *
// */
//
//package org.cangnova.cangjie.project.roots
//
//import com.intellij.navigation.ItemPresentation
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
//import com.intellij.openapi.roots.SyntheticLibrary
//import com.intellij.openapi.vfs.VirtualFile
//import org.cangnova.cangjie.icon.CangJieIcons
//import org.cangnova.cangjie.project.model.CjProject
//import org.cangnova.cangjie.project.service.cangjieProjectService
//import javax.swing.Icon
//
///**
// * 仓颉额外库根目录提供者
// *
// * 实现 [AdditionalLibraryRootsProvider] 接口，为 IntelliJ IDE 提供仓颉项目的额外库根目录。
// * 这使得 IDE 能够正确索引和识别仓颉项目中的所有代码，包括：
// * - 项目源码
// * - 依赖库
// * - 生成的代码
// *
// * 通过提供 [SyntheticLibrary]，IDE 可以：
// * - 在项目结构视图中显示仓颉项目
// * - 为仓颉代码提供代码补全和导航
// * - 正确处理依赖关系
// *
// * 该类会为每个 [CjProject] 创建一个 [CangJieSyntheticLibrary] 实例。
// *
// * ## Project Model 优先策略
// *
// * 此类是 **Project Model 的核心实现**，负责提供项目结构信息给 IDE。
// * 它优先于 Workspace Model 工作，确保即使 Workspace Model 同步失败，
// * IDE 仍能正确索引和识别仓颉代码。
// */
//class CangJieAdditionalLibraryRootsProvider : AdditionalLibraryRootsProvider() {
//
//    /**
//     * 获取额外的库根目录
//     *
//     * 遍历所有仓颉项目，为每个项目创建一个合成库。
//     *
//     * @param project IntelliJ 项目实例
//     * @return 合成库的集合
//     */
//    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
//        val projectService = project.cangjieProjectService
//
//        // 为每个 CjProject 创建一个 SyntheticLibrary
//        return   CangJieSyntheticLibrary(projectService.cjProject)
//
//    }
//}
//
///**
// * 仓颉合成库
// *
// * 代表一个仓颉项目在 IDE 中的合成库表示。
// * 合成库包含项目的所有可索引目录，使 IDE 能够正确识别和处理项目代码。
// *
// * ## 依赖关系支持
// *
// * 此类通过 [getDependencies] 方法提供模块间的依赖关系，
// * 使 IDE 能够正确解析跨模块的符号引用。
// *
// * @property cjProject 仓颉项目实例
// */
//private class CangJieSyntheticLibrary(
//    private val cjProject: CjProject
//) : SyntheticLibrary(), ItemPresentation {
//
//    /**
//     * 库的源码根目录
//     *
//     * 返回项目中所有可索引的目录，包括：
//     * - 模块源码目录
//     * - 模块资源目录
//     * - 模块输出目录
//     */
//    override fun getSourceRoots(): Collection<VirtualFile> {
//        return cjProject.indexableDirectories
//    }
//
//    /**
//     * 库的依赖关系
//     *
//     * 返回此项目依赖的其他合成库。
//     * 这允许 IDE 正确解析跨模块的符号引用，提供准确的代码补全和导航。
//     *
//     * 依赖关系的建立：
//     * 1. 遍历项目中的所有模块
//     * 2. 收集每个模块的依赖模块名称
//     * 3. 在同一 IntelliJ 项目中查找对应的 CjProject
//     * 4. 返回对应的 SyntheticLibrary 实例
//     *
//     * 注意：只返回同一 IntelliJ 项目中的依赖，跨项目依赖需要其他机制处理。
//     */
//    fun getDependencies(): Collection<SyntheticLibrary> {
//        val projectService = cjProject.intellijProject.cangjieProjectService
//        val dependencies = mutableSetOf<SyntheticLibrary>()
//
//        // 遍历项目中的所有模块
//        for (module in cjProject.modules) {
//            // 获取模块的依赖列表
//            for (dependency in module.dependencies) {
//                // 在项目服务中查找依赖的模块
//                val dependencyProject = projectService.allProjects
//                    .firstOrNull { it.findModule(dependency.moduleName) != null }
//
//                // 如果找到了依赖项目，将其合成库添加到依赖列表
//                if (dependencyProject != null && dependencyProject != cjProject) {
//                    dependencies.add(CangJieSyntheticLibrary(dependencyProject))
//                }
//            }
//        }
//
//        return dependencies
//    }
//
//    /**
//     * 库的展示名称
//     */
//    override fun getPresentableText(): String {
//        return "CangJie: ${cjProject.name}"
//    }
//
//    /**
//     * 库的图标
//     */
//    override fun getIcon(unused: Boolean): Icon {
//        return CangJieIcons.FILE
//    }
//
//    /**
//     * 相等性检查
//     *
//     * 基于项目根目录判断两个合成库是否相等
//     */
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is CangJieSyntheticLibrary) return false
//        return cjProject.rootDir == other.cjProject.rootDir
//    }
//
//    /**
//     * 哈希码
//     */
//    override fun hashCode(): Int {
//        return cjProject.rootDir.hashCode()
//    }
//}
