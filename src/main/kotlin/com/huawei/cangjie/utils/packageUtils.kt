package com.huawei.cangjie.utils

import com.huawei.cangjie.ide.cache.sourceRoot
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

fun CjFile.packageMatchesDirectoryOrImplicit() =
    packageFqName == getFqNameByDirectory() /*|| packageFqName == parent?.getFqNameWithImplicitPrefix()*/

fun PsiFile.isChildOf(directory: PsiDirectory): Boolean {
    var currentDirectory: PsiDirectory? = this.parent

    while (currentDirectory != null) {
        if (currentDirectory.virtualFile == directory.virtualFile) {
            return true
        }
        currentDirectory = currentDirectory.parentDirectory
    }
    return false
}

fun PsiFile.getFqNameByDirectory(): FqName {


    val singleFileSourcesTracker = SingleFileSourcesTracker.getInstance(project)
    val singleFileSourcePackageName = singleFileSourcesTracker.getPackageNameForSingleFileSource(virtualFile)
    singleFileSourcePackageName?.let { return FqName(it) }
    //    判断该文件是否在src目录下
//    val srcDirectory = Paths.get(project.basePath, "src")
//    if (srcDirectory != null && this.parent?.virtualFile == srcDirectory.virtualFile) {

//    }


    return parent?.getNonRootFqNameOrNull() ?: FqName.ROOT
//    return FqName.ROOT

}

private fun PsiDirectory.getNonRootFqNameOrNull(): FqName? {
    if (this.sourceRoot == null) return null
    var name = FqName(this.project.name)

    var _this = this

    val namelist = mutableListOf<String>()
    while (_this.virtualFile != sourceRoot) {
        namelist.add(_this.name)
        _this = _this.parentDirectory!!
    }

    if (namelist.isNotEmpty()) {
//        将namelist倒叙
        namelist.reverse()
        namelist.forEach {
            name = name.child(Name.identifier(it))
        }
    }

    return name
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
