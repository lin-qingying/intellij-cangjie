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

package org.cangnova.cangjie.buildsystem.impl.cjpm.utils

import org.cangnova.cangjie.cjpm.project.model.cjpmProjects
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import kotlin.io.path.Path

import com.intellij.psi.PsiDirectory

import java.nio.file.Path



/**
 * 获取当前文件归属的cjpm模块名与目录名
 */
fun PsiDirectory.getCjpmProjectDirectory(): Pair<String, Path> {

    val cjpmProjects = project.cjpmProjects
//获取所有cjpm项目的模块名称与根目录
    val projectDirectories: List<Pair<String, Path>> = cjpmProjects.allProjects.flatMap {
//        如果是工作空间
        if (it.isWorkspace) {
            (it.workspace?.packages?.mapNotNull {
                it.name to it.rootDirectory
            } ?: emptyList()) + listOf(project.name to Path(project.basePath ?: ""))
        } else {
            listOf(it.presentableName to it.manifest.parent)

        }
    }



    // 规范化当前目录路径
    val currentPath = this.virtualFile.path

    // 查找当前目录所属的项目
    return projectDirectories.find { (_, rootPath) ->
        currentPath.startsWith(rootPath.toString().replace("\\", "/"))
    } ?: ("" to Path(""))

}

/**
 * 获取当前目录（非根目录）的全限定名
 * 从当前目录向上遍历，直到达到源码根目录，将路径转换为FqName格式
 * @return 当前目录的全限定名，如果无法确定，则返回null
 */
//private fun PsiDirectory.getNonRootFqNameOrNull(): FqName? {
//
//    //    如果当前目录不属于任何源码根，则返回null
//    if (this.sourceRoot == null) return null
//    val cjpmProjectDirectory = getCjpmProjectDirectory()
//    //    初始化FqName为项目名 TODO 将项目名改为cjpm模块名
////    var name = FqName(this.project.name)
//    var name = FqName(cjpmProjectDirectory.first)
//    var _this = this
//
//    //    创建一个列表，用于存储从当前目录到源码根目录的路径
//    val namelist = mutableListOf<String>()
//    //    循环向上遍历目录，直到达到源码根目录
//    while (_this.virtualFile != sourceRoot) {
//        //    将当前目录名添加到列表中
//        namelist.add(_this.name)
//        //    移动到父目录
//        _this = _this.parentDirectory!!
//    }
//
//    //    如果列表不为空，则将列表倒序并构造FqName
//    if (namelist.isNotEmpty()) {
//        //        将namelist倒叙
//        namelist.reverse()
//        //        遍历列表，逐级构造FqName
//        namelist.forEach {
//            name = name.child(Name.identifier(it))
//        }
//    }
//
//    //    返回构造的FqName
//    return name
//}

data class CjpmProjectDirectory(val directory: String? = null, val parent: CjpmProjectDirectory? = null) {
    fun getFqName(): FqName {
        var name = FqName(this.directory!!)
        var _this = this
        val namelist = mutableListOf<String>()
        while (_this.parent != null) {
            namelist.add(_this.directory!!)
            _this = _this.parent
        }
        if (namelist.isNotEmpty()) {
            namelist.reverse()
            namelist.forEach {
                name = name.child(Name.identifier(it))
            }
        }
        return name
    }
}

//fun PsiDirectory.getPackage(): FqName? = JavaDirectoryService.getInstance()!!.getPackage(this)
//fun PsiDirectory.getFqNameWithImplicitPrefix(): FqName? {
//    val packageFqName = getNonRootFqNameOrNull() ?: return null
//    sourceRoot?.takeIf { !it.hasExplicitPackagePrefix(project) }?.let { sourceRoot ->
//        val implicitPrefix = PerModulePackageCacheService.getInstance(project).getImplicitPackagePrefix(sourceRoot)
//        return FqName.fromSegments((implicitPrefix.pathSegments() + packageFqName.pathSegments()).map { it.asString() })
//    }
//
//    return packageFqName
//}
