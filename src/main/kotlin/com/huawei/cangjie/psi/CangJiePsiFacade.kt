package com.huawei.cangjie.psi

import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.packgae.CangJiePackage
import com.huawei.cangjie.psi.packgae.CangJiePackageImpl
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
