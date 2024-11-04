package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjTypeStatement
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelClassByPackageIndex internal constructor() : StringStubIndexExtension<CjTypeStatement>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeStatement>(CjTypeStatement::class.java) {
        override val indexKey: StubIndexKey<String, CjTypeStatement> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieTopLevelClassByPackageIndex")
    }

    override fun getKey(): StubIndexKey<String, CjTypeStatement> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelClassByPackageIndex[fqName, project, scope]"))
    override fun get(fqName: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeStatement> {
        return Helper[fqName, project, scope]
    }
}
