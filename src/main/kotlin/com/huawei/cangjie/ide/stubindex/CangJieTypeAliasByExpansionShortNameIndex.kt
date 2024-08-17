package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjTypeAlias
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


class CangJieTypeAliasByExpansionShortNameIndex internal constructor() : StringStubIndexExtension<CjTypeAlias>() {
    companion object Helper : CangJieStringStubIndexHelper<CjTypeAlias>(CjTypeAlias::class.java) {
        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieTypeAliasByExpansionShortNameIndex = CangJieTypeAliasByExpansionShortNameIndex()

        override val indexKey: StubIndexKey<String, CjTypeAlias> =
            StubIndexKey.createIndexKey(CangJieTypeAliasByExpansionShortNameIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjTypeAlias> = indexKey

    @Deprecated(
        "Base method is deprecated",
        ReplaceWith("CangJieTypeAliasByExpansionShortNameIndex[key, project, scope]")
    )
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjTypeAlias> {
        return Helper[key, project, scope]
    }
}
