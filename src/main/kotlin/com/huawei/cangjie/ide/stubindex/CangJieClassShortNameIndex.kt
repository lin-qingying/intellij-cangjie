package com.huawei.cangjie.ide.stubindex

import com.huawei.cangjie.psi.CjClassOrStruct
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieClassShortNameIndex internal constructor() : StringStubIndexExtension<CjClassOrStruct>() {
    companion object Helper : CangJieStringStubIndexHelper<CjClassOrStruct>(CjClassOrStruct::class.java) {
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieClassShortNameIndex {
            return CangJieClassShortNameIndex()
        }

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieClassShortNameIndex = CangJieClassShortNameIndex()

        override val indexKey: StubIndexKey<String, CjClassOrStruct> =
            StubIndexKey.createIndexKey("com.huawei.cangjie.ide.stubindex.CangJieClassShortNameIndex")
    }

    override fun getKey(): StubIndexKey<String,CjClassOrStruct> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieClassShortNameIndex[key, project, scope]"))
    override fun get(shortName: String, project: Project, scope: GlobalSearchScope): Collection<CjClassOrStruct> {
        return Helper[shortName, project, scope]
    }
}
