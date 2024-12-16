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

package com.linqingying.cangjie.psi

import com.intellij.openapi.application.ReadActionProcessor
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.PossiblyDumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.psi.impl.file.PsiDirectoryOrFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.project.workspace.PackageOrigin
import com.linqingying.cangjie.ide.indices.CangJiePackageIndexUtils
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.packgae.CangJiePackage
import com.linqingying.cangjie.utils.roots.PackageIndex

abstract class CangJiePsiElementFinderBase : PossiblyDumbAware {
    open fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        return true
    }

    companion object {

        val EP =
            ProjectExtensionPointName<CangJiePsiElementFinderBase>("com.linqingying.cangjie.psi.elementFinder")

    }
}


/**
 * 为标准库特殊处理
 */
class CjpmStdlibPsiElementFinder(val project: Project) : CangJiePsiElementFinderBase(), DumbAware {
    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        val psiManager = PsiManager.getInstance(project)
        return psiPackage.qualifiedName?.let {

            val packageNames = it.split('.')
            val stdlibName = packageNames[0]


            project.cjpmProjects.currentCjpmProject?.workspace?.packages?.find { `package` ->
                `package`.name == stdlibName && `package`.origin == PackageOrigin.STDLIB

            }?.let a@{ `package` ->
                CangJiePackageIndexUtils.findFilesWithExactPackageByAllScope(
                    FqName(it), project
                ).firstOrNull()?.let { cjfile ->

                    (psiManager.findDirectory(cjfile.virtualFile)
                        ?: psiManager.findFile(cjfile.virtualFile))?.let { psiFile ->
                        PsiDirectoryOrFile(
                            psiFile,
                            psiManager as PsiManagerImpl,
                            cjfile.virtualFile
                        ).let { vfile ->
                            consumer.process(vfile)
                        }
                    }


                }


            }

        } != false
    }
}

/**
 * Cjpm 项目查找  a.b.c  查找a  模块查找
 */
/**
 * CjpmProjectPsiElementFinder 类用于在 CangJie 项目中查找 PSI 元素。
 * 它继承自 CangJiePsiElementFinderBase 并实现了 DumbAware 接口。
 * 主要功能是处理包目录的搜索和查找。
 *
 * @param project 当前的项目实例，用于访问项目相关的资源和配置。
 */
class CjpmProjectPsiElementFinder(val project: Project) : CangJiePsiElementFinderBase(), DumbAware {

    /**
     * 处理包目录的搜索和查找。
     * 该方法会根据包的限定名称在指定的搜索范围内查找对应的目录，并通过 consumer 处理这些目录。
     *
     * @param psiPackage 要处理的 CangJie 包对象。
     * @param scope 搜索的范围。
     * @param consumer 处理找到的目录的处理器。
     * @param includeLibrarySources 是否包括库源码的标志。
     * @return 如果处理成功则返回 true，否则返回 false。
     */
    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        // 获取当前项目的 PsiManager 实例，用于后续的 PSI 元素查找。
        val psiManager = PsiManager.getInstance(project)
        return psiPackage.qualifiedName?.let {

            // 将包的限定名称按点号分割，用于后续的处理。
            val packageNames = it.split('.').drop(1)

            // 如果分割后的包名称列表大小大于 1，则直接返回 true，表示处理成功。
            if (packageNames.size > 1) return true

            // 尝试在当前的 Cjpm 项目中查找匹配的包。
            project.cjpmProjects.currentCjpmProject?.workspace?.packages?.find { `package` ->
                `package`.name == it

            }?.let { `package` ->
                // 如果找到了匹配的包，则尝试获取其 manifest 文件对应的 PSI 元素。
                `package`.manifestPath?.let a@{ it1 ->
                    (psiManager.findDirectory(it1) ?: psiManager.findFile(it1))?.let { psiFile ->
                        // 创建 PsiDirectoryOrFile 对象并处理它。
                        return@a PsiDirectoryOrFile(
                            psiFile,
                            psiManager as PsiManagerImpl,
                            it1
                        ).let { vfile ->
                            consumer.process(vfile)
                        }
                    }

                }

            }

        } != false
    }
}


/**
 * Cjpm 包查找   a.b.c 查找c
 */

/**
 * CjpmPackagePsiElementFinder 类用于在项目中查找特定包名的 PSI 元素。
 * 它继承自 CangJiePsiElementFinderBase，并实现了 DumbAware 接口，使其能在智能模式下工作。
 *
 * @param project 当前项目实例，用于访问项目相关的 PSI 管理器和包索引。
 */
class CjpmPackagePsiElementFinder(val project: Project) : CangJiePsiElementFinderBase(), DumbAware {

    /**
     * 处理包目录的方法，根据包名查找并处理相应的目录。
     *
     * @param psiPackage 要处理的包对象，用于获取包的全限定名。
     * @param scope 搜索范围，用于限定查找的目录范围。
     * @param consumer 目录处理器，用于处理找到的目录。
     * @param includeLibrarySources 是否包括库源码，控制是否在库源码中进行查找。
     * @return 返回是否成功处理了所有目录。如果返回 false，则表示处理被中止。
     */
    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        // 获取当前项目的 PsiManager 实例，用于后续查找目录。
        val psiManager = PsiManager.getInstance(project)
        return psiPackage.qualifiedName?.let {
            // 处理包名，去除最外层的包名（模块名），并重新组合包名。
            val packageName =   it.split('.').drop(1).let { sarr ->
                if (sarr.isEmpty()) {
                    // 如果包名为空，则直接返回 true，表示处理完成。
                    return true
                } else {
                    // 否则，将包名数组重新组合为带点的包名字符串。
                    sarr.joinToString(".")
                }
            }

            // 使用包名查找对应的目录，并对每个目录进行处理。
            PackageIndex.getInstance(project)
                .getDirsByPackageName(packageName, includeLibrarySources)
                .forEach(object : ReadActionProcessor<VirtualFile?>() {
                    /**
                     * 在读取操作中处理目录。
                     * 如果目录在指定的搜索范围内，则查找对应的 PSI 目录并进行处理。
                     *
                     * @param dir 目录的 VirtualFile 对象。
                     * @return 返回是否继续处理其他目录。如果返回 false，则中止处理。
                     */
                    override fun processInReadAction(dir: VirtualFile?): Boolean {
                        if (!scope.contains(dir!!)) return true
                        val psiDir = psiManager.findDirectory(dir)
                        return psiDir == null || consumer.process(psiDir)
                    }
                })
        } != false
    }
}
