package com.huawei.cangjie.ide.indices

import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.huawei.cangjie.ide.vfilefinder.NAME

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex


object CangJiePackageIndexUtils{
    fun getSubPackageFqNames(
        packageFqName: FqName,
        scope: GlobalSearchScope,
        nameFilter: (Name) -> Boolean
    ): Collection<FqName> = getSubpackages(packageFqName, scope, nameFilter)

    fun findFilesWithExactPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Collection<CjFile> = CangJieExactPackagesIndex.get(packageFqName.asString(), project, searchScope)
    /**
     * Return all direct subpackages of package [fqName].
     *
     * I.e. if there are packages `a.b`, `a.b.c`, `a.c`, `a.c.b` for `fqName` = `a` it returns
     * `a.b` and `a.c`
     *
     * Follow the contract of [com.intellij.psi.PsiElementFinder#getSubPackages]
     */
    fun getSubpackages(fqName: FqName, scope: GlobalSearchScope, nameFilter: (Name) -> Boolean): Collection<FqName> {
        val result = hashSetOf<FqName>()

        FileBasedIndex.getInstance().processValues(
           NAME, fqName, null,
            FileBasedIndex.ValueProcessor { _, subPackageName ->
                if (subPackageName != null && nameFilter(subPackageName)) {
                    result.add(fqName.child(subPackageName))
                }
                true
            }, scope
        )

        return result
    }

    /**
     * Return true if exists package with exact [fqName] OR there are some subpackages of [fqName]
     */
    fun packageExists(fqName: FqName, project: Project): Boolean =
        packageExists(fqName, GlobalSearchScope.allScope(project))


    /**
     * Return true if package [packageFqName] exists or some subpackages of [packageFqName] exist in [searchScope]
     */
    fun packageExists(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Boolean {
        val a= !FileBasedIndex.getInstance().processValues(
            NAME,
            packageFqName,
            null,
            FileBasedIndex.ValueProcessor { _, _ -> false },
            searchScope
        )
        return a
    }
}
