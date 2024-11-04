package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjTypeAlias
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTypeAliasShortNameIndex internal constructor() : StringStubIndexExtension<CjTypeAlias>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeAlias>(CjTypeAlias::class.java) {
        override val indexKey: StubIndexKey<String, CjTypeAlias> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieTypeAliasShortNameIndex")
    }

    override fun getKey(): StubIndexKey<String, CjTypeAlias> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTypeAliasShortNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeAlias> {
        return Helper[key, project, scope]
    }
}
