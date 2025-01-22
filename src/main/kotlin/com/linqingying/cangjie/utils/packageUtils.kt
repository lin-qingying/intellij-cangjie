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

package com.linqingying.cangjie.utils

import kotlin.io.path.Path
import com.linqingying.cangjie.ide.cache.sourceRoot
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import java.nio.file.Path

/**
 * 检查文件的包名是否与目录结构匹配，或者隐式匹配
 *
 * 此函数旨在确保文件的包全名（packageFqName）与根据其所在目录计算得到的全名（getFqNameByDirectory()）一致
 * 或者与父目录的全名加上隐式前缀匹配（此逻辑在注释中被省略）
 * 这种检查有助于维护项目中文件组织的一致性和可预测性
 *
 * @return Boolean 表示包名是否匹配目录结构或隐式前缀
 */
fun CjFile.packageMatchesDirectoryOrImplicit() =
    packageFqName == getFqNameByDirectory() /*|| packageFqName == parent?.getFqNameWithImplicitPrefix()*/

/**
 * 判断当前PsiFile是否是指定PsiDirectory的子文件
 *
 * 该函数通过不断向上遍历当前文件的父目录，来确定当前文件是否位于指定目录内
 *
 * @param directory 指定的PsiDirectory对象，用于检查当前文件是否是其子文件
 * @return 如果当前文件是指定目录的子文件，则返回true；否则返回false
 */
fun PsiFile.isChildOf(directory: PsiDirectory): Boolean {
    // 从当前文件的父目录开始检查
    var currentDirectory: PsiDirectory? = this.parent

    // 循环直到当前目录为空（即到达文件树的根部）
    while (currentDirectory != null) {
        // 如果当前目录与指定目录的虚拟文件相同，则当前文件是指定目录的子文件
        if (currentDirectory.virtualFile == directory.virtualFile) {
            return true
        }
        // 将当前目录更新为其父目录，继续向上检查
        currentDirectory = currentDirectory.parentDirectory
    }
    // 如果遍历结束都没有找到相同的目录，说明当前文件不是指定目录的子文件
    return false
}

/**
 * 获取当前文件的全限定名
 * 如果文件是单文件源，则使用SingleFileSourcesTracker获取包名
 * 否则，返回文件所在目录的全限定名，如果不存在，则返回根FqName
 * @return 当前文件的全限定名
 */
fun PsiFile.getFqNameByDirectory(): FqName {


    //    使用SingleFileSourcesTracker获取单文件源的包名
    val singleFileSourcesTracker = SingleFileSourcesTracker.getInstance(project)
    val singleFileSourcePackageName = singleFileSourcesTracker.getPackageNameForSingleFileSource(virtualFile)
    singleFileSourcePackageName?.let { return FqName(it) }

    //    如果上述方法未能获取包名，则尝试通过判断文件位置来确定FqName
    //    判断该文件是否在src目录下
//    val srcDirectory = Paths.get(project.basePath, "src")
//    if (srcDirectory != null && this.parent?.virtualFile == srcDirectory.virtualFile) {
//    }

    //    如果文件不在src目录下，则返回文件所在目录的全限定名，如果不存在父目录，则返回根FqName
    return parent?.getNonRootFqNameOrNull() ?: FqName.ROOT
//    return FqName.ROOT

}

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
        currentPath.startsWith(rootPath.toString())
    } ?: ("" to Path(""))

}

/**
 * 获取当前目录（非根目录）的全限定名
 * 从当前目录向上遍历，直到达到源码根目录，将路径转换为FqName格式
 * @return 当前目录的全限定名，如果无法确定，则返回null
 */
private fun PsiDirectory.getNonRootFqNameOrNull(): FqName? {

    //    如果当前目录不属于任何源码根，则返回null
    if (this.sourceRoot == null) return null
    val cjpmProjectDirectory = getCjpmProjectDirectory()
    //    初始化FqName为项目名 TODO 将项目名改为cjpm模块名
//    var name = FqName(this.project.name)
    var name = FqName(cjpmProjectDirectory.first)
    var _this = this

    //    创建一个列表，用于存储从当前目录到源码根目录的路径
    val namelist = mutableListOf<String>()
    //    循环向上遍历目录，直到达到源码根目录
    while (_this.virtualFile != sourceRoot) {
        //    将当前目录名添加到列表中
        namelist.add(_this.name)
        //    移动到父目录
        _this = _this.parentDirectory!!
    }

    //    如果列表不为空，则将列表倒序并构造FqName
    if (namelist.isNotEmpty()) {
        //        将namelist倒叙
        namelist.reverse()
        //        遍历列表，逐级构造FqName
        namelist.forEach {
            name = name.child(Name.identifier(it))
        }
    }

    //    返回构造的FqName
    return name
}

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
