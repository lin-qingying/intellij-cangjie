package com.huawei.cangjie.psi.packgae

import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CangJiePsiFacade
import com.intellij.lang.Language
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.Processors
import com.intellij.util.containers.ContainerUtil
import kotlin.concurrent.Volatile


class CangJiePackageImpl(

    manager: PsiManager,
    val qualifiedName: FqName,
    val scope: GlobalSearchScope?
) : AbstractCangJiePackage(
    manager, qualifiedName.asString()
) {
    override fun copy(): PsiElement {

        return CangJiePackageImpl(
            manager, qualifiedName, scope
        )
    }

    override fun isValid(): Boolean {
        return scope?.let {
            CangJiePackageIndexUtils.packageExists(
                qualifiedName, it

            )
        } ?: false
    }
}

abstract class AbstractCangJiePackage(

    manager: PsiManager,
    qualifiedName: String
) : PsiPackageBase(
    manager, qualifiedName
), CangJiePackage, Queryable {
    override fun getLanguage(): Language = CangJieLanguage
    private val facade: CangJiePsiFacade
        get() {
            return CangJiePsiFacade.getInstance(project)
        }

    @Volatile
    private lateinit var myDirectoriesWithLibSources: CachedValue<Collection<PsiDirectory>>

    @Volatile
    private lateinit var myDirectories: CachedValue<Collection<PsiDirectory>>
    private fun createCachedDirectories(includeLibrarySources: Boolean): CachedValue<Collection<PsiDirectory>> {
        return CachedValuesManager.getManager(project).createCachedValue({
            val result: Collection<PsiDirectory> = ArrayList()
            val processor = Processors.cancelableCollectProcessor(result)
            facade.processPackageDirectories(this, allScope(), processor, includeLibrarySources)
            CachedValueProvider.Result.create(
                result, CangJiePackageImplementationHelper.getDirectoryCachedValueDependencies(
                    this
                )
            )
        }, false)
    }

    override fun navigate(requestFocus: Boolean) {
        CangJiePackageImplementationHelper.navigate(this, requestFocus)

    }


    protected fun allScope(): GlobalSearchScope {
        return GlobalSearchScope.allScope(
            project
        )

    }

    override fun getAllDirectories(scope: GlobalSearchScope?): MutableCollection<PsiDirectory> {
        if (scope!!.isForceSearchingInLibrarySources) {
            if (!::myDirectoriesWithLibSources.isInitialized) {
                myDirectoriesWithLibSources = createCachedDirectories(true)
            }
            return ContainerUtil.filter(
                myDirectoriesWithLibSources.value
            ) { d: PsiDirectory -> scope.contains(d.virtualFile) }
        } else {
            if (!::myDirectories.isInitialized) {
                myDirectories = createCachedDirectories(false)
            }
            return ContainerUtil.filter(
                myDirectories.value
            ) { d: PsiDirectory -> scope.contains(d.virtualFile) }
        }
    }

    override fun findPackage(qName: String): AbstractCangJiePackage? {

        return facade.findPackage(qName) as? AbstractCangJiePackage


    }
}
