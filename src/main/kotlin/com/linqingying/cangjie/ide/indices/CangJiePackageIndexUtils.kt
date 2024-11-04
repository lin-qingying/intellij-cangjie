package com.linqingying.cangjie.ide.indices

import com.linqingying.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.linqingying.cangjie.ide.vfilefinder.NAME
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex


object CangJiePackageIndexUtils {
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


    fun findFilesWithExactPackageByAllScope(
        packageFqName: FqName,

        project: Project
    ): Collection<CjFile> =
        CangJieExactPackagesIndex.get(packageFqName.asString(), project, GlobalSearchScope.allScope(project))

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
            { _, subPackageName ->
                if (subPackageName != null && nameFilter(subPackageName)) {
                    result.add(fqName.child(subPackageName))
                }
                true
            }, scope
        )

        return result
    }


    fun getSubPackageFqNamesByAllScope(
        packageFqName: FqName,
        project: Project
    ): Collection<FqName> = getSubpackages(packageFqName, GlobalSearchScope.allScope(project)) {
        true
    }

    fun getSubPackageFqNames(
        packageFqName: FqName,
        scope: GlobalSearchScope
    ): Collection<FqName> = getSubpackages(packageFqName, scope) {
        true
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
        val a = !FileBasedIndex.getInstance().processValues(
            NAME,
            packageFqName,
            null,
            FileBasedIndex.ValueProcessor { _, _ -> false },
            searchScope
        )
        return a
    }
}
