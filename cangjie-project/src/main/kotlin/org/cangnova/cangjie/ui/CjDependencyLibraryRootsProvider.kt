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
//package org.cangnova.cangjie.dependency.ui
//
//import com.intellij.navigation.ItemPresentation
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
//import com.intellij.openapi.roots.SyntheticLibrary
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.openapi.vfs.VirtualFileManager
//import org.cangnova.cangjie.dependency.extensions.dependencies
//import org.cangnova.cangjie.dependency.model.CjDependency
//import org.cangnova.cangjie.dependency.model.CjResolvedDependency
//import org.cangnova.cangjie.dependency.service.CjDependencyService
//import org.cangnova.cangjie.project.service.CjProjectsService
//import javax.swing.Icon
//
///**
// * 仓颉依赖库根提供者
// *
// * 负责为 IntelliJ 项目视图提供外部库根目录
// */
//class CjDependencyLibraryRootsProvider : AdditionalLibraryRootsProvider() {
//
//    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
//        val libraries = mutableListOf<SyntheticLibrary>()
//
//        // 获取所有仓颉项目
//        val projectsService = project.getService(CjProjectsService::class.java) ?: return libraries
//        val dependencyService = ApplicationManager.getApplication().getService(CjDependencyService::class.java)
//            ?: return libraries
//
//        // 为每个项目的每个模块解析依赖
//        projectsService.allProjects.forEach { cjProject ->
//            cjProject.modules.forEach { module ->
//                module.dependencies.forEach { dependency ->
//                    // 解析依赖
//                    val resolved = dependencyService.resolveDependency(dependency, project)
//                    if (resolved != null && resolved.isResolved) {
//                        // 创建合成库
//                        libraries.add(CjDependencyLibrary(dependency, resolved))
//                    }
//                }
//            }
//        }
//
//        return libraries
//    }
//}
//
///**
// * 仓颉依赖合成库
// *
// * 代表一个已解析的依赖包,用于在外部库视图中显示
// */
//private class CjDependencyLibrary(
//    private val dependency: CjDependency,
//    private val resolved: CjResolvedDependency
//) : SyntheticLibrary(), ItemPresentation {
//
//    override fun getSourceRoots(): Collection<VirtualFile> {
//        val roots = mutableListOf<VirtualFile>()
//        val vfsManager = VirtualFileManager.getInstance()
//
//        // 添加已解析包的源码根
////        resolved.resolvedPackage?.let { pkg ->
////            pkg.sourcePaths.forEach { path ->
////                vfsManager.findFileByNioPath(path)?.let { roots.add(it) }
////            }
////        }
////
////        // 添加已解析库的源码根
////        resolved.resolvedLibrary?.let { lib ->
////            lib.sourcePaths.forEach { path ->
////                vfsManager.findFileByNioPath(path)?.let { roots.add(it) }
////            }
////        }
//
//        return roots
//    }
//
//    override fun getBinaryRoots(): Collection<VirtualFile> {
//        val roots = mutableListOf<VirtualFile>()
//        val vfsManager = VirtualFileManager.getInstance()
//
//        // 添加已解析包的二进制根
////        resolved.resolvedPackage?.let { pkg ->
////            pkg.binaryPaths.forEach { path ->
////                vfsManager.findFileByNioPath(path)?.let { roots.add(it) }
////            }
////        }
////
////        // 添加已解析库的二进制根
////        resolved.resolvedLibrary?.let { lib ->
////            lib.binaryPaths.forEach { path ->
////                vfsManager.findFileByNioPath(path)?.let { roots.add(it) }
////            }
////        }
//
//        return roots
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is CjDependencyLibrary) return false
//        return dependency == other.dependency
//    }
//
//    override fun hashCode(): Int {
//        return dependency.hashCode()
//    }
//
//    // ItemPresentation implementation for display in External Libraries
//
//    override fun getPresentableText(): String {
//        val name = dependency.group?.let { "${it}:${dependency.name}" } ?: dependency.name
//        return "$name:${dependency.version}"
//    }
//
//    override fun getLocationString(): String? {
//        return dependency.scope.name.lowercase()
//    }
//
//    override fun getIcon(unused: Boolean): Icon? {
//        // TODO: 返回合适的图标
//        return null
//    }
//}