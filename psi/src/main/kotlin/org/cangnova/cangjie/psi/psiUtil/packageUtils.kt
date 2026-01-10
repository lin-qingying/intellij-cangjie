/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.name.*

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
    return /*parent?.getNonRootFqNameOrNull() ?:*/ FqName.ROOT
//    return FqName.ROOT

}