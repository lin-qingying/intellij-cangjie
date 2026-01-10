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

package org.cangnova.cangjie.psi.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDirectoryContainer
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.psi.impl.file.PsiDirectoryImpl
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CangJiePsiFacade
import org.cangnova.cangjie.psi.CjPsiDirectory

/**
 * 仓颉语言的目录工厂实现
 *
 * 此类扩展了 IntelliJ 平台的 PsiDirectoryFactory，为仓颉语言提供自定义的目录处理逻辑。
 * 主要功能包括：
 * - 识别仓颉源码根目录
 * - 计算目录的完全限定包名
 * - 验证包名的合法性
 * - 提供包目录容器
 */
internal class CangJieDirectoryFactory(private val project: Project) : PsiDirectoryFactory() {

    private val psiManager = PsiManagerEx.getInstanceEx(project)
    private val projectFileIndex = ProjectFileIndex.getInstance(project)

    /**
     * 创建 PsiDirectory 实例
     *
     * @param file 虚拟文件对象
     * @return 对应的 PsiDirectory 实例
     */
    override fun createDirectory(file: VirtualFile): PsiDirectory {
        return CjPsiDirectory(psiManager, file)
    }

    /**
     * 获取目录的完全限定名称
     *
     * 对于仓颉源码根下的目录，返回其包名（例如 "std.collection"）
     * 对于非源码根目录，返回相对路径
     *
     * @param directory PSI 目录对象
     * @param presentable 是否返回用户友好的表示形式
     * @return 目录的完全限定名称
     */
    override fun getQualifiedName(directory: PsiDirectory, presentable: Boolean): String {
        val virtualFile = directory.virtualFile

        // 如果不是包目录，返回默认行为
        if (!isPackage(directory)) {
            return if (presentable) {
                virtualFile.presentableUrl
            } else {
                virtualFile.path
            }
        }

        // 获取源码根
        val sourceRoot = projectFileIndex.getSourceRootForFile(virtualFile)
            ?: return virtualFile.path

        // 计算相对于源码根的路径
        val relativePath = virtualFile.path.removePrefix(sourceRoot.path).trim('/', '\\')

        // 如果是源码根本身，返回根包名
        if (relativePath.isEmpty()) {
            return ""
        }

        // 将路径转换为包名（路径分隔符转换为点）
        return relativePath.replace('/', '.').replace('\\', '.')
    }

    /**
     * 获取目录容器
     *
     * 对于仓颉包目录，返回对应的 CangJiePackage 实例
     * CangJiePackage 实现了 PsiDirectoryContainer 接口
     *
     * @param directory PSI 目录对象
     * @return CangJiePackage 实例，如果不是包目录则返回 null
     */
    override fun getDirectoryContainer(directory: PsiDirectory): PsiDirectoryContainer? {
        if (!isPackage(directory)) {
            return null
        }

        val packageName = getQualifiedName(directory, false)
        val fqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)

        // 使用 CangJiePsiFacade 查找或创建包
        val facade = CangJiePsiFacade.getInstance(project)
        return facade.findPackage(fqName)
    }

    /**
     * 判断目录是否是包目录
     *
     * 仓颉包目录的条件：
     * 1. 位于仓颉源码根下（SOURCE 或 TEST_SOURCE）
     * 2. 包含至少一个 .cj 文件，或者
     * 3. 包含子目录且子目录是包目录
     *
     * @param directory PSI 目录对象
     * @return 如果是包目录返回 true，否则返回 false
     */
    override fun isPackage(directory: PsiDirectory): Boolean {
        val virtualFile = directory.virtualFile

        // 检查是否在仓颉源码根下
        val sourceRoot = projectFileIndex.getSourceRootForFile(virtualFile)
            ?: return false

        // 检查是否在源码目录中（不是测试资源或普通资源）
        if (!projectFileIndex.isInSource(virtualFile)) {
            return false
        }

        // 检查是否包含 .cj 文件
//        val hasCangjieFiles = virtualFile.children.any {
//            !it.isDirectory && it.extension == "cj"
//        }

//        if (hasCangjieFiles) {
//            return true
//        }

        // 检查子目录是否是包目录（避免递归深度过大）
        // 只检查直接子目录，不递归检查
//        val hasPackageSubdirs = virtualFile.children.any { child ->
//            if (!child.isDirectory) return@any false
//
//            // 检查子目录是否包含 .cj 文件
//            child.children.any { !it.isDirectory && it.extension == "cj" }
//        }

        return true
    }

    /**
     * 验证包名是否合法
     *
     * 仓颉包名规则：
     * 1. 可以为空（根包）
     * 2. 由标识符组成，用点分隔
     * 3. 每个标识符必须是合法的仓颉标识符
     *
     * @param name 包名字符串
     * @return 如果包名合法返回 true，否则返回 false
     */
    override fun isValidPackageName(name: String?): Boolean {
        if (name == null) {
            return false
        }

        // 空字符串表示根包，是合法的
        if (name.isEmpty()) {
            return true
        }

        // 分割包名并验证每个部分
        val parts = name.split('.')

        return parts.all { part ->
            isValidIdentifier(part)
        }
    }

    /**
     * 验证标识符是否合法
     *
     * 仓颉标识符规则：
     * 1. 不能为空
     * 2. 第一个字符必须是字母或下划线
     * 3. 后续字符可以是字母、数字或下划线
     *
     * @param identifier 标识符字符串
     * @return 如果标识符合法返回 true，否则返回 false
     */
    private fun isValidIdentifier(identifier: String): Boolean {
        if (identifier.isEmpty()) {
            return false
        }

        val first = identifier[0]
        if (!first.isLetter() && first != '_') {
            return false
        }

        return identifier.drop(1).all { c ->
            c.isLetterOrDigit() || c == '_'
        }
    }
}
