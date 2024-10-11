package com.huawei.cangjie.psi

import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.packgae.CangJiePackage
import com.huawei.cangjie.utils.roots.PackageIndex
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
            ProjectExtensionPointName<CangJiePsiElementFinderBase>("com.huawei.cangjie.psi.elementFinder")

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
                        return@a PsiDirectoryOrFile(
                            psiFile,
                            psiManager as PsiManagerImpl,
                            cjfile.virtualFile
                        ).let { vfile ->
                            consumer.process(vfile)
                        }
                    }


                }


            }

        } ?: true
    }
}

/**
 * Cjpm 项目查找
 */
class CjpmProjectPsiElementFinder(val project: Project) : CangJiePsiElementFinderBase(), DumbAware {

    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        val psiManager = PsiManager.getInstance(project)
        return psiPackage.qualifiedName?.let {

            val packageNames = it.split('.').drop(1)

            if (packageNames.size > 1) return true

            project.cjpmProjects.currentCjpmProject?.workspace?.packages?.find { `package` ->
                `package`.name == it

            }?.let { `package` ->
                `package`.manifestPath?.let a@{ it1 ->
                    (psiManager.findDirectory(it1) ?: psiManager.findFile(it1))?.let { psiFile ->
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

        } ?: true
    }
}


/**
 * Cjpm 包查找
 */

class CjpmPackagePsiElementFinder(val project: Project) : CangJiePsiElementFinderBase(), DumbAware {

    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean
    ): Boolean {
        val psiManager = PsiManager.getInstance(project)
        return psiPackage.qualifiedName?.let {


            val packageName = it.split('.').drop(1).let { sarr ->
                if (sarr.isEmpty()) {
                    return true
                } else {
                    sarr.joinToString(".")
                }
            }


            PackageIndex.getInstance(project)
                .getDirsByPackageName(packageName, includeLibrarySources)
                .forEach(object : ReadActionProcessor<VirtualFile?>() {
                    override fun processInReadAction(dir: VirtualFile?): Boolean {
                        if (!scope.contains(dir!!)) return true
                        val psiDir = psiManager.findDirectory(dir)
                        return psiDir == null || consumer.process(psiDir)
                    }
                })
        } ?: true
    }
}



