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

package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.ide.indices.CangJiePackageIndexUtils
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.packgae.CangJiePackage
import cn.cangnova.cangjie.psi.packgae.CangJiePackageImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor


abstract class CangJiePsiFacade {

    fun findPackage(fqName: String): CangJiePackage? {
        return findPackage(FqName(fqName))
    }

    abstract fun findPackage(fqName: FqName): CangJiePackage?

    abstract fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean

    companion object {


        fun getInstance(project: Project): CangJiePsiFacade {
            return project.service<CangJiePsiFacade>()
        }
    }
}

class DefaultCangJiePsiFacade(val project: Project) : CangJiePsiFacade() {
    private val psiManager = PsiManager.getInstance(project)
    private fun filteredFinders(): List<CangJiePsiElementFinderBase> {
        return DumbService.getInstance(project)
            .filterByDumbAwareness(CangJiePsiElementFinderBase.EP.getPoint(project).extensionList)
    }

    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        for (finder in filteredFinders()) {
            if (!finder.processPackageDirectories(psiPackage, scope, consumer, includeLibrarySources)) {
                return false
            }
        }
        return true
    }


    override fun findPackage(fqName: FqName): CangJiePackage? {
        val allScope = GlobalSearchScope.allScope(project)
        return if (CangJiePackageIndexUtils.packageExists(fqName, allScope)) {
            CangJiePackageImpl(psiManager, fqName, allScope)
        } else null

    }
}
