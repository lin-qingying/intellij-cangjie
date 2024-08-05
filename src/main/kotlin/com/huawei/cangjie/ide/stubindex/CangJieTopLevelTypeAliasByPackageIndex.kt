package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjTypeAlias
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelTypeAliasByPackageIndex internal constructor() : StringStubIndexExtension<CjTypeAlias>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeAlias>(CjTypeAlias::class.java) {
        override val indexKey: StubIndexKey<String, CjTypeAlias> =
            StubIndexKey.createIndexKey(CangJieTopLevelTypeAliasByPackageIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjTypeAlias> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelTypeAliasByPackageIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeAlias> {
        return Helper[key, project, scope]
    }
}
