package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.psi.CjTypeAlias
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTopLevelTypeAliasFqNameIndex internal constructor() : StringStubIndexExtension<CjTypeAlias>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeAlias>(CjTypeAlias::class.java) {
        override val indexKey: StubIndexKey<String, CjTypeAlias> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieTopLevelTypeAliasFqNameIndex")

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieTopLevelTypeAliasFqNameIndex = CangJieTopLevelTypeAliasFqNameIndex()
    }

    override fun getKey(): StubIndexKey<String, CjTypeAlias> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieTopLevelTypeAliasFqNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeAlias> {
        return Helper[key, project, scope]
    }
}
